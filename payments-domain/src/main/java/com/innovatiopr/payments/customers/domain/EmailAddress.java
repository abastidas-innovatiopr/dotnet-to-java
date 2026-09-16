package com.innovatiopr.payments.customers.domain;

import java.util.Locale;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * A validated email address.
 *
 * <p>Validation lives in the domain, not only in a Bean Validation annotation on the HTTP request. The
 * annotation stops malformed input at the transport edge; this type guarantees that <em>no</em> code path,
 * including a database import or a test fixture, can construct a customer with a nonsense address.
 */
public record EmailAddress(String value) {

    private static final Pattern PATTERN = Pattern.compile("^[^@\\s]+@[^@\\s.]+(\\.[^@\\s.]+)+$");
    private static final int MAX_LENGTH = 254;

    public EmailAddress {
        Objects.requireNonNull(value, "value");
    }

    public static EmailAddress create(String raw) {
        if (raw == null || raw.isBlank()) {
            throw CustomerErrors.invalidEmail("Email address is required");
        }
        String normalised = raw.trim().toLowerCase(Locale.ROOT);
        if (normalised.length() > MAX_LENGTH) {
            throw CustomerErrors.invalidEmail("Email address exceeds " + MAX_LENGTH + " characters");
        }
        if (!PATTERN.matcher(normalised).matches()) {
            throw CustomerErrors.invalidEmail("'" + raw + "' is not a valid email address");
        }
        return new EmailAddress(normalised);
    }

    /** Rehydration from storage, where the value is known to have been validated already. */
    public static EmailAddress fromStorage(String value) {
        return new EmailAddress(value);
    }

    @Override
    public String toString() {
        return value;
    }
}
