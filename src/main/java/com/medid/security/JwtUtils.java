package com.medid.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import java.security.Key;
import java.util.UUID;
import java.util.Date;

@Component
public class JwtUtils {
    private final String jwtSecret;
    private final long accessExpirationMs;
    private final long refreshExpirationMs;

    private final Key key;

    public JwtUtils(
            @Value("${security.jwt.secret:MediD_Secret_Key_2026_Secure_Long_String_For_HS256}") String jwtSecret,
            @Value("${security.jwt.access-expiration-ms:1800000}") long accessExpirationMs,
            @Value("${security.jwt.refresh-expiration-ms:604800000}") long refreshExpirationMs
    ) {
        this.jwtSecret = jwtSecret;
        this.accessExpirationMs = accessExpirationMs;
        this.refreshExpirationMs = refreshExpirationMs;
        this.key = Keys.hmacShaKeyFor(this.jwtSecret.getBytes());
    }

    public String generateAccessToken(String username, String fingerprint, String userAgentHash) {
        return buildToken(username, fingerprint, userAgentHash, "access", accessExpirationMs);
    }

    public String generateRefreshToken(String username, String fingerprint, String userAgentHash) {
        return buildToken(username, fingerprint, userAgentHash, "refresh", refreshExpirationMs);
    }

    private String buildToken(String username, String fingerprint, String userAgentHash, String type, long ttlMs) {
        return Jwts.builder()
                .setSubject(username)
                .claim("fp", fingerprint)
                .claim("uah", userAgentHash)
                .claim("typ", type)
                .setId(UUID.randomUUID().toString())
                .setIssuedAt(new Date())
                .setExpiration(new Date((new Date()).getTime() + ttlMs))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    public String getUsernameFromToken(String token) {
        return Jwts.parserBuilder().setSigningKey(key).build()
                .parseClaimsJws(token).getBody().getSubject();
    }

    public Date getExpirationFromToken(String token) {
        try {
            return Jwts.parserBuilder().setSigningKey(key).build()
                    .parseClaimsJws(token).getBody().getExpiration();
        } catch (Exception e) {
            return null;
        }
    }

    public long getAccessExpirationSeconds() {
        return Math.max(1, accessExpirationMs / 1000);
    }

    public long getRefreshExpirationSeconds() {
        return Math.max(1, refreshExpirationMs / 1000);
    }

    public boolean validateAccessToken(String token, String fingerprint, String userAgentHash) {
        return validateToken(token, fingerprint, userAgentHash, "access");
    }

    public boolean validateRefreshToken(String token, String fingerprint, String userAgentHash) {
        return validateToken(token, fingerprint, userAgentHash, "refresh");
    }

    private boolean validateToken(String token, String fingerprint, String userAgentHash, String expectedType) {
        try {
            Claims claims = Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token).getBody();
            String claimFingerprint = claims.get("fp", String.class);
            String claimUaHash = claims.get("uah", String.class);
            String claimType = claims.get("typ", String.class);
            return expectedType.equals(claimType)
                    && fingerprint != null
                    && fingerprint.equals(claimFingerprint)
                    && userAgentHash != null
                    && userAgentHash.equals(claimUaHash);
        } catch (Exception e) {
            return false;
        }
    }
}
