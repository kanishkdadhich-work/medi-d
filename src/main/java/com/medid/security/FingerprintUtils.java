package com.medid.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.UUID;

public final class FingerprintUtils {
    private FingerprintUtils() {}

    public static String generateFingerprintValue() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    public static String fromUserAgent(String userAgent) {
        String source = userAgent == null ? "" : userAgent;
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(source.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : hash) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (Exception e) {
            return "";
        }
    }
}
