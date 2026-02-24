package com.medid.controller;

import com.medid.entity.User;
import com.medid.enums.Role;
import com.medid.repository.UserRepository;
import com.medid.security.FingerprintUtils;
import com.medid.security.JwtUtils;
import com.medid.security.RevokedTokenService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
@Slf4j
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;
    private final RevokedTokenService revokedTokenService;

    public AuthController(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtUtils jwtUtils, RevokedTokenService revokedTokenService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtils = jwtUtils;
        this.revokedTokenService = revokedTokenService;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> credentials, HttpServletRequest request, HttpServletResponse response) {
        String username = credentials.get("username");
        String password = credentials.get("password");

        // Explicit payload validation avoids noisy server errors.
        if (username == null || username.isBlank() || password == null || password.isBlank()) {
            return ResponseEntity.status(400).body(Map.of(
                    "error_code", "VALIDATION_ERROR",
                    "message", "username and password are required"
            ));
        }

        User user = userRepository.findByUsername(username).orElse(null);
        if (user == null) {
            return ResponseEntity.status(401).body(Map.of(
                    "error_code", "AUTH_FAILED",
                    "message", "Invalid credentials"
            ));
        }

        if (!user.isEnabled()) {
            return ResponseEntity.status(403).body("User account is disabled");
        }

        if (user.getRole() == null) {
            user.setRole(Role.RECEPTIONIST);
            userRepository.save(user);
        }

        // --- DEBUG CLAUSE ---
        if (log.isDebugEnabled()) {
            log.debug("User: {}, Match: {}", username, passwordEncoder.matches(password, user.getPassword()));
        }
        if (passwordEncoder.matches(password, user.getPassword())) {
            String fingerprint = FingerprintUtils.generateFingerprintValue();
            String userAgentHash = FingerprintUtils.fromUserAgent(request.getHeader("User-Agent"));
            String accessToken = jwtUtils.generateAccessToken(username, fingerprint, userAgentHash);
            String refreshToken = jwtUtils.generateRefreshToken(username, fingerprint, userAgentHash);

            // Access token cookie: short-lived and bound to browser fingerprint.
            ResponseCookie accessCookie = ResponseCookie.from("medid_token", accessToken)
                    .httpOnly(true)       // Prevents JS access (Anti-XSS)
                    .secure(false)         // Set to true in production with HTTPS
                    .path("/")             // Available for all routes
                    .maxAge(jwtUtils.getAccessExpirationSeconds())
                    .sameSite("Lax")       // CSRF protection
                    .build();

            // Refresh token cookie: longer-lived token for access token rotation.
            ResponseCookie refreshCookie = ResponseCookie.from("medid_refresh", refreshToken)
                    .httpOnly(true)
                    .secure(false)
                    .path("/")
                    .maxAge(jwtUtils.getRefreshExpirationSeconds())
                    .sameSite("Lax")
                    .build();

            ResponseCookie fingerprintCookie = ResponseCookie.from("medid_fp", fingerprint)
                    .httpOnly(true)
                    .secure(false)
                    .path("/")
                    .maxAge(jwtUtils.getRefreshExpirationSeconds())
                    .sameSite("Lax")
                    .build();

            return ResponseEntity.ok()
                    .header(HttpHeaders.SET_COOKIE, accessCookie.toString())
                    .header(HttpHeaders.SET_COOKIE, refreshCookie.toString())
                    .header(HttpHeaders.SET_COOKIE, fingerprintCookie.toString())
                    .body(Map.of(
                            "message", "Login successful",
                            "role", user.getRole().name()
                    ));
        } else {
            return ResponseEntity.status(401).body(Map.of(
                    "error_code", "AUTH_FAILED",
                    "message", "Invalid credentials"
            ));
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpServletRequest request) {
        String accessToken = extractToken(request, "medid_token");
        if (accessToken == null || accessToken.isBlank()) {
            accessToken = extractBearerToken(request);
        }
        String refreshToken = extractToken(request, "medid_refresh");

        if (accessToken != null) {
            revokedTokenService.revoke(accessToken, jwtUtils.getExpirationFromToken(accessToken));
        }
        if (refreshToken != null) {
            revokedTokenService.revoke(refreshToken, jwtUtils.getExpirationFromToken(refreshToken));
        }

        ResponseCookie accessCookie = ResponseCookie.from("medid_token", "")
                .httpOnly(true)
                .path("/")
                .maxAge(0)
                .build();

        ResponseCookie refreshCookie = ResponseCookie.from("medid_refresh", "")
                .httpOnly(true)
                .path("/")
                .maxAge(0)
                .build();

        ResponseCookie fingerprintCookie = ResponseCookie.from("medid_fp", "")
                .httpOnly(true)
                .path("/")
                .maxAge(0)
                .build();

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, accessCookie.toString())
                .header(HttpHeaders.SET_COOKIE, refreshCookie.toString())
                .header(HttpHeaders.SET_COOKIE, fingerprintCookie.toString())
                .body("Logged out");
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(HttpServletRequest request) {
        String refreshToken = extractToken(request, "medid_refresh");
        String fingerprint = extractToken(request, "medid_fp");
        String userAgentHash = FingerprintUtils.fromUserAgent(request.getHeader("User-Agent"));
        if (refreshToken == null || refreshToken.isBlank()) {
            return ResponseEntity.status(401).body(Map.of("error_code", "AUTH_FAILED", "message", "Refresh token missing"));
        }
        if (fingerprint == null || fingerprint.isBlank()) {
            return ResponseEntity.status(401).body(Map.of("error_code", "AUTH_FAILED", "message", "Fingerprint missing"));
        }
        if (revokedTokenService.isRevoked(refreshToken)) {
            return ResponseEntity.status(401).body(Map.of("error_code", "AUTH_FAILED", "message", "Refresh token revoked"));
        }

        if (!jwtUtils.validateRefreshToken(refreshToken, fingerprint, userAgentHash)) {
            return ResponseEntity.status(401).body(Map.of("error_code", "AUTH_FAILED", "message", "Invalid refresh token"));
        }

        String username = jwtUtils.getUsernameFromToken(refreshToken);
        String newAccessToken = jwtUtils.generateAccessToken(username, fingerprint, userAgentHash);
        String newRefreshToken = jwtUtils.generateRefreshToken(username, fingerprint, userAgentHash);

        revokedTokenService.revoke(refreshToken, jwtUtils.getExpirationFromToken(refreshToken));

        ResponseCookie accessCookie = ResponseCookie.from("medid_token", newAccessToken)
                .httpOnly(true)
                .secure(false)
                .path("/")
                .maxAge(jwtUtils.getAccessExpirationSeconds())
                .sameSite("Lax")
                .build();
        ResponseCookie refreshCookie = ResponseCookie.from("medid_refresh", newRefreshToken)
                .httpOnly(true)
                .secure(false)
                .path("/")
                .maxAge(jwtUtils.getRefreshExpirationSeconds())
                .sameSite("Lax")
                .build();

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, accessCookie.toString())
                .header(HttpHeaders.SET_COOKIE, refreshCookie.toString())
                .body(Map.of("message", "Token refreshed"));
    }

    @GetMapping("/me")
    public ResponseEntity<?> me(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof User user)) {
            return ResponseEntity.status(401).body(Map.of("message", "Unauthenticated"));
        }
        if (user.getRole() == null) {
            user.setRole(Role.RECEPTIONIST);
            userRepository.save(user);
        }
        return ResponseEntity.ok(Map.of(
                "username", user.getUsername(),
                "role", user.getRole().name()
        ));
    }

    @PostMapping("/signup")
    public ResponseEntity<?> signup(@RequestBody User user) {
        if (userRepository.findByUsername(user.getUsername()).isPresent()) {
            return ResponseEntity.badRequest().body("Username already exists");
        }

        if (user.getRole() == null) {
            user.setRole(Role.RECEPTIONIST);
        }

        if (Role.ADMIN.equals(user.getRole())) {
            return ResponseEntity.status(403).body("Signup cannot create ADMIN users.");
        }

        // Encodes the password correctly before saving
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        userRepository.save(user);
        return ResponseEntity.ok("User registered successfully");
    }

    private String extractToken(HttpServletRequest request, String cookieName) {
        if (request.getCookies() == null) return null;
        for (Cookie cookie : request.getCookies()) {
            if (cookieName.equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        return null;
    }

    private String extractBearerToken(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        return null;
    }
}
