package com.medid.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class LoginRateLimitFilter extends OncePerRequestFilter {

    // Simple in-memory sliding-window limiter for /api/auth/login.
    // Intention: reject brute-force bursts early with HTTP 429.
    private static final int MAX_REQUESTS_PER_SECOND = 20;
    private static final long WINDOW_MILLIS = 1_000L;

    private final Map<String, Deque<Long>> requestHistory = new ConcurrentHashMap<>();

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !"/api/auth/login".equals(request.getRequestURI()) || !"POST".equalsIgnoreCase(request.getMethod());
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String ip = request.getRemoteAddr();
        long now = Instant.now().toEpochMilli();

        Deque<Long> timestamps = requestHistory.computeIfAbsent(ip, key -> new ArrayDeque<>());
        synchronized (timestamps) {
            while (!timestamps.isEmpty() && now - timestamps.peekFirst() > WINDOW_MILLIS) {
                timestamps.pollFirst();
            }

            if (timestamps.size() >= MAX_REQUESTS_PER_SECOND) {
                response.setStatus(429);
                response.setContentType("application/json");
                response.getWriter().write("{\"error_code\":\"RATE_LIMITED\",\"message\":\"Too many login attempts. Please retry shortly.\"}");
                return;
            }

            timestamps.addLast(now);
        }

        filterChain.doFilter(request, response);
    }
}
