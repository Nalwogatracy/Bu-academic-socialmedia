package com.finalyearproject.controller;

import com.finalyearproject.model.User;
import com.finalyearproject.service.*;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@RestController
@RequestMapping("/sse")
public class SseController {

    // Store one emitter per user (keyed by userId)
    private final Map<Long, SseEmitter> emitters = new ConcurrentHashMap<>();

    private final UserService userService;
    private final MessageService messageService;
    private final NotificationService notificationService;
    private final AssignmentService assignmentService;

    public SseController(UserService userService,
                         MessageService messageService,
                         NotificationService notificationService,
                         AssignmentService assignmentService) {
        this.userService = userService;
        this.messageService = messageService;
        this.notificationService = notificationService;
        this.assignmentService = assignmentService;
    }

    // ── Student connects to SSE stream ────────────────────────────────────────
    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Transactional(readOnly = true)
    public SseEmitter stream(Authentication authentication) {
        User user = userService.findByEmail(authentication.getName());

        // Timeout after 5 minutes — browser reconnects automatically
        SseEmitter emitter = new SseEmitter(5 * 60 * 1000L);

        // Store emitter for this user
        emitters.put(user.getId(), emitter);

        // Clean up on complete/timeout/error
        emitter.onCompletion(() -> emitters.remove(user.getId()));
        emitter.onTimeout(()    -> emitters.remove(user.getId()));
        emitter.onError(e       -> emitters.remove(user.getId()));

        // Send initial counts immediately on connect
        try {
            emitter.send(SseEmitter.event()
                .name("badges")
                .data(buildBadgePayload(user)));
        } catch (IOException e) {
            emitters.remove(user.getId());
        }

        return emitter;
    }

    // ── Called by services to push update to a specific user ─────────────────
    public void pushBadgeUpdate(User user) {
        SseEmitter emitter = emitters.get(user.getId());
        if (emitter == null) return; // user not online

        try {
            emitter.send(SseEmitter.event()
                .name("badges")
                .data(buildBadgePayload(user)));
        } catch (IOException e) {
            emitters.remove(user.getId());
        }
    }

    // ── Push a toast notification to a specific user ──────────────────────────
    public void pushNotification(User user, String title, String message) {
        SseEmitter emitter = emitters.get(user.getId());
        if (emitter == null) return;

        try {
            emitter.send(SseEmitter.event()
                .name("notification")
                .data(Map.of("title", title, "message", message)));
        } catch (IOException e) {
            emitters.remove(user.getId());
        }
    }

    private Map<String, Object> buildBadgePayload(User user) {
        return Map.of(
            "unreadMessages",      messageService.countUnread(user),
            "unreadNotifications", notificationService.countUnread(user),
            "pendingAssignments",  assignmentService.countPending(user)
        );
    }
}