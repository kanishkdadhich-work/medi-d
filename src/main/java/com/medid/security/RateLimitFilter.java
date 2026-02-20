package com.medid.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private final Map<String, Bucket> store = new ConcurrentHashMap<>();
    private final int MAX = 120; // max requests
    private final long WINDOW_MS = 60_000; // window in ms

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String key = request.getRemoteAddr();
        Bucket b = store.computeIfAbsent(key, k -> new Bucket());
        synchronized (b) {
            long now = Instant.now().toEpochMilli();
            if (now - b.windowStart > WINDOW_MS) { b.windowStart = now; b.count.set(0); }
            if (b.count.incrementAndGet() > MAX) {
                response.setStatus(429);
                response.getWriter().write("Too many requests");
                return;
            }
        }
        filterChain.doFilter(request, response);
    }

    static class Bucket { long windowStart = Instant.now().toEpochMilli(); AtomicInteger count = new AtomicInteger(0); }
}
