package com.innovatiopr.payments.accounts.domain;

import java.security.SecureRandom;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * A customer-facing account number.
 *
 * <p>Distinct from {@code AccountId}: the id is an internal surrogate key that never appears on a
 * statement, while the number is the value a human quotes. Keeping them separate means the public
 * identifier can be reissued or reformatted without touching a single foreign key.
 */
public record AccountNumber(String value) {

    private static final Pattern PATTERN = Pattern.compile("^\\d{12}$");
    private static final SecureRandom RANDOM = new SecureRandom();

    public AccountNumber {
        Objects.requireNonNull(value, "value");
    }

    public static AccountNumber create(String raw) {
        if (raw == null || raw.isBlank()) {
            throw AccountErrors.invalidAccountNumber("Account number is required");
        }
        String trimmed = raw.trim();
        if (!PATTERN.matcher(trimmed).matches()) {
            throw AccountErrors.invalidAccountNumber("Account number must be exactly 12 digits");
        }
        return new AccountNumber(trimmed);
    }

    /** Generates a random 12-digit number. Collisions are caught by the unique index on {@code accounts}. */
    public static AccountNumber generate() {
        StringBuilder builder = new StringBuilder(12);
        for (int i = 0; i < 12; i++) {
            builder.append(RANDOM.nextInt(10));
        }
        return new AccountNumber(builder.toString());
    }

    public static AccountNumber fromStorage(String value) {
        return new AccountNumber(value);
    }

    @Override
    public String toString() {
        return value;
    }
}
