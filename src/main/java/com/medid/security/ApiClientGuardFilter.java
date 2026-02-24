package com.medid.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Locale;

@Component
public class ApiClientGuardFilter extends OncePerRequestFilter {

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String uri = request.getRequestURI();
        if (uri == null || !uri.startsWith("/api/")) return true;
        // Keep auth bootstrap endpoints reachable.
        return uri.startsWith("/api/auth/login") || uri.startsWith("/api/auth/signup") || uri.startsWith("/api/auth/refresh");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        if (hasAuthMaterial(request) && isBlockedClient(request)) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setContentType("application/json");
            response.getWriter().write("{\"error_code\":\"CLIENT_BLOCKED\",\"message\":\"Authenticated API access is browser-only for this session.\"}");
            return;
        }

        filterChain.doFilter(request, response);
    }

    private boolean hasAuthMaterial(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return true;
        }

        Cookie[] cookies = request.getCookies();
        if (cookies == null) return false;
        for (Cookie cookie : cookies) {
            if ("medid_token".equals(cookie.getName()) || "medid_refresh".equals(cookie.getName())) {
                return true;
            }
        }
        return false;
    }

    private boolean isBlockedClient(HttpServletRequest request) {
        String userAgent = request.getHeader("User-Agent");
        if (isToolUserAgent(userAgent)) {
            return true;
        }

        // Browser-driven requests carry fetch metadata; API clients usually don't.
        String fetchSite = request.getHeader("Sec-Fetch-Site");
        String fetchMode = request.getHeader("Sec-Fetch-Mode");
        boolean missingBrowserFetchHeaders = fetchSite == null || fetchMode == null;
        if (missingBrowserFetchHeaders) {
            return true;
        }

        // Postman commonly adds this header automatically.
        if (request.getHeader("Postman-Token") != null) {
            return true;
        }

        // Hard-block bearer-header replay even if cookie is present.
        String authHeader = request.getHeader("Authorization");
        return authHeader != null && authHeader.startsWith("Bearer ");
    }

    private boolean isToolUserAgent(String userAgent) {
        if (userAgent == null || userAgent.isBlank()) return false;
        String ua = userAgent.toLowerCase(Locale.ROOT);
        return ua.contains("postmanruntime")
                || ua.contains("insomnia")
                || ua.contains("curl/")
                || ua.contains("httpie")
                || ua.contains("python-requests")
                || ua.contains("okhttp");
    }
}
