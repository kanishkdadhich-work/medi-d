package com.medid.controller;

import com.medid.security.JwtTokenProvider;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.*;

import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private AuthenticationManager authManager;

    @Autowired
    private JwtTokenProvider tokenProvider;

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest req) {
        Authentication a = authManager.authenticate(new UsernamePasswordAuthenticationToken(req.username, req.password));
        String roles = a.getAuthorities().stream().map(GrantedAuthority::getAuthority).collect(Collectors.joining(","));
        String token = tokenProvider.createToken(a.getName(), roles);
        return ResponseEntity.ok(new TokenResponse(token));
    }

    @Data
    static class LoginRequest {
        @NotBlank(message = "Username is required")
        String username;
        @NotBlank(message = "Password is required")
        String password;
    }

    @Data
    static class TokenResponse { String token; TokenResponse(String t){ this.token=t;} }
}
