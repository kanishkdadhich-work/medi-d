package com.medid.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final Logger logger = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    private final JwtTokenProvider tokenProvider;
    private final UserDetailsService userDetailsService;

    public JwtAuthenticationFilter(JwtTokenProvider tokenProvider,
                                   UserDetailsService userDetailsService) {
        this.tokenProvider = tokenProvider;
        this.userDetailsService = userDetailsService;
        logger.info("JwtAuthenticationFilter initialized with UserDetailsService: " + 
                   (userDetailsService != null ? userDetailsService.getClass().getName() : "null"));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String header = request.getHeader("Authorization");
        logger.debug("JwtAuthenticationFilter: Authorization header = " + 
                    (header != null ? header.substring(0, Math.min(30, header.length())) + "..." : "null"));
        
        if (header != null && header.startsWith("Bearer ")) {
            String token = header.substring(7);
            logger.debug("JwtAuthenticationFilter: Found Bearer token, length = " + token.length());
            try {
                boolean valid = tokenProvider.validateToken(token);
                logger.debug("JwtAuthenticationFilter: Token validation result = " + valid);
                
                if (valid) {
                    String username = tokenProvider.getUsername(token);
                    logger.debug("JwtAuthenticationFilter: Token username = " + username);
                    
                    UserDetails user = userDetailsService.loadUserByUsername(username);
                    logger.debug("JwtAuthenticationFilter: Loaded user = " + username + ", authorities = " + user.getAuthorities());
                    
                    var auth = new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());
                    auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    
                    // Get the current context or create a new one
                    SecurityContext context = SecurityContextHolder.getContext();
                    context.setAuthentication(auth);
                    
                    logger.info("JwtAuthenticationFilter: Successfully authenticated user = " + username);
                    logger.debug("JwtAuthenticationFilter: SecurityContext authentication set to " + auth);
                } else {
                    logger.debug("JwtAuthenticationFilter: Token validation failed");
                }
            } catch (Exception e) {
                logger.error("JwtAuthenticationFilter: Error validating token: " + e.getMessage(), e);
            }
        } else {
            logger.debug("JwtAuthenticationFilter: No Authorization header or not Bearer token");
        }

        filterChain.doFilter(request, response);
    }
}
