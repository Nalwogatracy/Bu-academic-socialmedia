package com.finalyearproject.config;

import com.finalyearproject.model.User;
import com.finalyearproject.service.UserService;
import com.finalyearproject.service.UserStatusService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class UserActivityInterceptor implements HandlerInterceptor {

    private final UserStatusService userStatusService;
    private final UserService userService;

    public UserActivityInterceptor(UserStatusService userStatusService,
                                   UserService userService) {
        this.userStatusService = userStatusService;
        this.userService = userService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request,
                             HttpServletResponse response,
                             Object handler) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        // Only update for authenticated, non-anonymous users
        // Skip static resources and SSE stream to avoid spam
        String uri = request.getRequestURI();
        boolean isStaticResource = uri.contains("/css/") || uri.contains("/js/")
                || uri.contains("/images/") || uri.contains("/favicon");
        boolean isSseStream = uri.contains("/sse/stream");

        if (auth != null && auth.isAuthenticated()
                && !auth.getPrincipal().equals("anonymousUser")
                && !isStaticResource
                && !isSseStream) {
            try {
                User user = userService.findByEmail(auth.getName());
                if (user != null) {
                    userStatusService.markOnline(user);
                }
            } catch (Exception e) {
                // Don't break the request if status update fails
                System.err.println("Failed to update user status: " + e.getMessage());
            }
        }
        return true;
    }
}