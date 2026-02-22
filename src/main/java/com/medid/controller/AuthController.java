package com.medid.controller;

import com.medid.entity.User;
import com.medid.enums.Role;
import com.medid.repository.UserRepository;
import com.medid.security.JwtUtils;
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

    public AuthController(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtUtils jwtUtils) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtils = jwtUtils;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> credentials, HttpServletResponse response) {
        String username = credentials.get("username");
        String password = credentials.get("password");

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

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
            String jwt = jwtUtils.generateToken(username);

            // Create HTTP-Only Cookie
            ResponseCookie cookie = ResponseCookie.from("medid_token", jwt)
                    .httpOnly(true)       // Prevents JS access (Anti-XSS)
                    .secure(false)         // Set to true in production with HTTPS
                    .path("/")             // Available for all routes
                    .maxAge(3600)          // 1 hour expiry for auto-login
                    .sameSite("Lax")       // CSRF protection
                    .build();

            return ResponseEntity.ok()
                    .header(HttpHeaders.SET_COOKIE, cookie.toString())
                    .body(Map.of("message", "Login successful", "role", user.getRole().name(), "token", jwt));
        } else {
            return ResponseEntity.status(401).body("Invalid credentials");
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout() {
        // Clear the cookie by setting maxAge to 0
        ResponseCookie cookie = ResponseCookie.from("medid_token", "")
                .httpOnly(true)
                .path("/")
                .maxAge(0)
                .build();
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body("Logged out");
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
}
