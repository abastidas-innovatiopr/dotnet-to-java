package com.innovatiopr.payments.payments.domain;

import java.util.Objects;
import java.util.UUID;

/**
 * Identity of a {@code PaymentTransaction}.
 *
 * <p>Kept in the module's internal {@code domain} package rather than its published root, because no other
 * module needs it: the Ledger deliberately references operations through its own
 * {@code PostingReference}. Publishing a type nothing consumes only widens the module's surface area.
 */
public record TransactionId(UUID value) {

    public TransactionId {
        Objects.requireNonNull(value, "value");
    }

    public static TransactionId generate() {
        return new TransactionId(UUID.randomUUID());
    }

    public static TransactionId of(UUID value) {
        return new TransactionId(value);
    }

    public static TransactionId parse(String raw) {
        return new TransactionId(UUID.fromString(raw));
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
