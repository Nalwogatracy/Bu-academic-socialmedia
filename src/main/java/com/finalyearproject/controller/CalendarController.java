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

import java.time.LocalDate;
import java.util.*;

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

    private String prepareCalendarView(List<CalendarEvent> events, User user, Model model) {
        // Group events by date (pre-processed for Thymeleaf compatibility)
        Map<LocalDate, List<CalendarEvent>> grouped = new TreeMap<>();
        for (CalendarEvent e : events) {
            LocalDate date = e.getDateTime().toLocalDate();
            grouped.computeIfAbsent(date, k -> new ArrayList<>()).add(e);
        }
        model.addAttribute("user", user);
        model.addAttribute("groupedEvents", grouped);
        model.addAttribute("eventCount", events.size());
        model.addAttribute("unreadMessages", messageService.countUnread(user));
        model.addAttribute("unreadNotifications", notificationService.countUnread(user));
        return "calendar";
    }

    @GetMapping("/student/calendar")
    public String studentCalendar(Authentication auth, Model model) {
        User user = userService.findByEmail(auth.getName());
        List<CalendarEvent> events = calendarService.getEventsForStudent(user);
        return prepareCalendarView(events, user, model);
    }

    @GetMapping("/lecturer/calendar")
    public String lecturerCalendar(Authentication auth, Model model) {
        User user = userService.findByEmail(auth.getName());
        List<CalendarEvent> events = calendarService.getEventsForLecturer(user);
        return prepareCalendarView(events, user, model);
    }

    @GetMapping("/admin/calendar")
    public String adminCalendar(Authentication auth, Model model) {
        User user = userService.findByEmail(auth.getName());
        List<CalendarEvent> events = calendarService.getEventsForAdmin();
        return prepareCalendarView(events, user, model);
    }
}
