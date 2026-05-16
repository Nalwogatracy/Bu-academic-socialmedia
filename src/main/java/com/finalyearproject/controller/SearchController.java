package com.finalyearproject.controller;

import com.finalyearproject.model.User;
import com.finalyearproject.service.MessageService;
import com.finalyearproject.service.NotificationService;
import com.finalyearproject.service.SearchService;
import com.finalyearproject.service.UserService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Map;

@Controller
public class SearchController {

    private final SearchService searchService;
    private final UserService userService;
    private final MessageService messageService;
    private final NotificationService notificationService;

    public SearchController(SearchService searchService, UserService userService,
                            MessageService messageService, NotificationService notificationService) {
        this.searchService = searchService;
        this.userService = userService;
        this.messageService = messageService;
        this.notificationService = notificationService;
    }

    @GetMapping("/search")
    public String search(@RequestParam("q") String query,
                         Authentication authentication,
                         Model model) {
        User user = userService.findByEmail(authentication.getName());
        Map<String, Object> results = searchService.search(query, user);

        model.addAttribute("user", user);
        model.addAttribute("results", results);
        model.addAttribute("query", query);
        model.addAttribute("unreadMessages", messageService.countUnread(user));
        model.addAttribute("unreadNotifications", notificationService.countUnread(user));

        model.addAttribute("role", user.getRole().name().toLowerCase());
        return "search-results";
    }
}
