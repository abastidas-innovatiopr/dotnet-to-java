package com.innovatiopr.payments.shared.application;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * Produces the canonical fingerprint of a request body used for idempotency-key replay detection.
 *
 * <p>Hashing rather than storing the body keeps account numbers and amounts out of a table that is read
 * on every retry, and gives a fixed-width column to index.
 */
public final class RequestHasher {

    private RequestHasher() {
    }

    public static String sha256(String canonicalRequest) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(canonicalRequest.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            // SHA-256 is mandated by the Java platform; absence is a broken JVM, not a business condition.
            throw new IllegalStateException("SHA-256 is unavailable", e);
        }
    }
}
