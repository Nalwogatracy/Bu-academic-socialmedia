package com.finalyearproject.config;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Order(1)
public class RateLimitingFilter implements Filter {

    private final Map<String, SlidingWindow> attempts = new ConcurrentHashMap<>();

    private static final int MAX_LOGIN_ATTEMPTS = 5;
    private static final int MAX_REGISTER_ATTEMPTS = 3;
    private static final int MAX_FORGOT_ATTEMPTS = 3;
    private static final Duration WINDOW = Duration.ofMinutes(15);

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest req = (HttpServletRequest) request;
        String path = req.getRequestURI();
        String method = req.getMethod();

        int limit = switch (path) {
            case "/login" -> method.equals("POST") ? MAX_LOGIN_ATTEMPTS : Integer.MAX_VALUE;
            case "/register" -> MAX_REGISTER_ATTEMPTS;
            case "/forgot-password" -> MAX_FORGOT_ATTEMPTS;
            default -> Integer.MAX_VALUE;
        };

        if (limit != Integer.MAX_VALUE) {
            String ip = getClientIp(req);
            SlidingWindow window = attempts.computeIfAbsent(ip, k -> new SlidingWindow());
            if (window.isExceeded(limit)) {
                HttpServletResponse res = (HttpServletResponse) response;
                res.setStatus(429);
                res.setContentType("text/plain");
                res.getWriter().write("Too many requests. Please try again in 15 minutes.");
                return;
            }
            window.record();
        }

        chain.doFilter(request, response);
    }

    private String getClientIp(HttpServletRequest req) {
        String xff = req.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) return xff.split(",")[0].trim();
        return req.getRemoteAddr();
    }

    private static class SlidingWindow {
        private final long[] timestamps = new long[20];
        private int idx = 0;
        private int count = 0;

        synchronized boolean isExceeded(int limit) {
            purge();
            return count >= limit;
        }

        synchronized void record() {
            purge();
            if (count < timestamps.length) {
                timestamps[idx] = System.currentTimeMillis();
                idx = (idx + 1) % timestamps.length;
                count++;
            }
        }

        private void purge() {
            long cutoff = System.currentTimeMillis() - WINDOW.toMillis();
            int newCount = 0;
            for (int i = 0; i < count; i++) {
                int pos = (idx - count + i + timestamps.length) % timestamps.length;
                if (timestamps[pos] > cutoff) {
                    timestamps[newCount] = timestamps[pos];
                    newCount++;
                }
            }
            count = newCount;
            idx = count;
        }
    }
}
