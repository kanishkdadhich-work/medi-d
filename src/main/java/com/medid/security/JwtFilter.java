package com.medid.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class JwtFilter extends OncePerRequestFilter {

    private final JwtUtils jwtUtils;
    private final UserDetailsService userDetailsService;
    private final RevokedTokenService revokedTokenService;

    public JwtFilter(JwtUtils jwtUtils, UserDetailsService userDetailsService, RevokedTokenService revokedTokenService) {
        this.jwtUtils = jwtUtils;
        this.userDetailsService = userDetailsService;
        this.revokedTokenService = revokedTokenService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String token = null;
        String fingerprint = null;
        String userAgentHash = FingerprintUtils.fromUserAgent(request.getHeader("User-Agent"));

        // 1) Prefer HTTP-only auth cookie.
        if (request.getCookies() != null) {
            for (Cookie cookie : request.getCookies()) {
                if ("medid_token".equals(cookie.getName())) {
                    token = cookie.getValue();
                }
                if ("medid_fp".equals(cookie.getName())) {
                    fingerprint = cookie.getValue();
                }
            }
        }

        // 2) If token is valid, not revoked, and fingerprint-bound, authenticate request.
        if (token != null
                && fingerprint != null
                && !fingerprint.isBlank()
                && !revokedTokenService.isRevoked(token)
                && jwtUtils.validateAccessToken(token, fingerprint, userAgentHash)) {
            String username = jwtUtils.getUsernameFromToken(token);
            UserDetails userDetails = userDetailsService.loadUserByUsername(username);

            UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                    userDetails, null, userDetails.getAuthorities());

            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

            // Marks the current request as authenticated for downstream authorization rules.
            SecurityContextHolder.getContext().setAuthentication(authentication);
        }

        filterChain.doFilter(request, response);
    }
}
