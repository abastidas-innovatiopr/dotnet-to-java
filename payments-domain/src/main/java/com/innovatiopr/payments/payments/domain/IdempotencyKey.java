package com.innovatiopr.payments.payments.domain;

import java.util.Objects;
import java.util.regex.Pattern;

/**
 * A client-supplied key that makes a money-moving request safe to retry.
 *
 * <p>The client generates it (a UUID is the usual choice) and resends the <em>same</em> key when a request
 * times out or the connection drops. The server guarantees that a given key moves money at most once.
 */
public record IdempotencyKey(String value) {

    public static final int MAX_LENGTH = 255;
    private static final Pattern PATTERN = Pattern.compile("^[A-Za-z0-9._:-]{8,255}$");

    public IdempotencyKey {
        Objects.requireNonNull(value, "value");
    }

    public static IdempotencyKey create(String raw) {
        if (raw == null || raw.isBlank()) {
            throw TransferErrors.missingIdempotencyKey();
        }
        String trimmed = raw.trim();
        if (!PATTERN.matcher(trimmed).matches()) {
            throw TransferErrors.invalidIdempotencyKey(
                    "Idempotency key must be 8-255 characters of letters, digits, '.', '_', ':' or '-'");
        }
        return new IdempotencyKey(trimmed);
    }

    public static IdempotencyKey fromStorage(String value) {
        return new IdempotencyKey(value);
    }

    @Override
    public String toString() {
        return value;
    }
}
