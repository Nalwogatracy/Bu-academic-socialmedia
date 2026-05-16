package com.finalyearproject.controller;

import com.finalyearproject.model.CalendarEvent;
import com.finalyearproject.model.User;
import com.finalyearproject.service.CalendarService;
import com.finalyearproject.service.MessageService;
import com.finalyearproject.service.NotificationService;
import com.finalyearproject.service.UserService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@Controller
public class CalendarController {

    private final CalendarService calendarService;
    private final UserService userService;
    private final MessageService messageService;
    private final NotificationService notificationService;

    public CalendarController(CalendarService calendarService, UserService userService,
                              MessageService messageService, NotificationService notificationService) {
        this.calendarService = calendarService;
        this.userService = userService;
        this.messageService = messageService;
        this.notificationService = notificationService;
    }

    @GetMapping("/student/calendar")
    public String studentCalendar(Authentication auth, Model model) {
        User user = userService.findByEmail(auth.getName());
        List<CalendarEvent> events = calendarService.getEventsForStudent(user);
        model.addAttribute("user", user);
        model.addAttribute("events", events);
        model.addAttribute("unreadMessages", messageService.countUnread(user));
        model.addAttribute("unreadNotifications", notificationService.countUnread(user));
        return "calendar";
    }

    @GetMapping("/lecturer/calendar")
    public String lecturerCalendar(Authentication auth, Model model) {
        User user = userService.findByEmail(auth.getName());
        List<CalendarEvent> events = calendarService.getEventsForLecturer(user);
        model.addAttribute("user", user);
        model.addAttribute("events", events);
        model.addAttribute("unreadMessages", messageService.countUnread(user));
        model.addAttribute("unreadNotifications", notificationService.countUnread(user));
        return "calendar";
    }

    @GetMapping("/admin/calendar")
    public String adminCalendar(Authentication auth, Model model) {
        User user = userService.findByEmail(auth.getName());
        List<CalendarEvent> events = calendarService.getEventsForAdmin();
        model.addAttribute("user", user);
        model.addAttribute("events", events);
        model.addAttribute("unreadMessages", messageService.countUnread(user));
        model.addAttribute("unreadNotifications", notificationService.countUnread(user));
        return "calendar";
    }
}
