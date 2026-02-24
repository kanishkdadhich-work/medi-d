package com.medid.security;

import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class RevokedTokenService {
    private final Map<String, Long> revokedTokens = new ConcurrentHashMap<>();

    public void revoke(String token, Date expiresAt) {
        if (token == null || token.isBlank() || expiresAt == null) return;
        revokedTokens.put(token, expiresAt.getTime());
        cleanupExpired();
    }

    public boolean isRevoked(String token) {
        if (token == null || token.isBlank()) return false;
        cleanupExpired();
        return revokedTokens.containsKey(token);
    }

    private void cleanupExpired() {
        long now = System.currentTimeMillis();
        revokedTokens.entrySet().removeIf(entry -> entry.getValue() <= now);
    }
}
