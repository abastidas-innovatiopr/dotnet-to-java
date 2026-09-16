package com.innovatiopr.payments.payments.domain;

import java.util.Objects;

/** Free-text description a customer attaches to a payment, for example "Rent payment". */
public record TransactionReference(String value) {

    public static final int MAX_LENGTH = 140;

    public TransactionReference {
        Objects.requireNonNull(value, "value");
    }

    public static TransactionReference create(String raw) {
        String normalised = raw == null ? "" : raw.trim();
        if (normalised.length() > MAX_LENGTH) {
            throw TransferErrors.invalidReference("Reference must be at most " + MAX_LENGTH + " characters");
        }
        return new TransactionReference(normalised);
    }

    public static TransactionReference empty() {
        return new TransactionReference("");
    }

    public static TransactionReference fromStorage(String value) {
        return new TransactionReference(value == null ? "" : value);
    }

    @Override
    public String toString() {
        return value;
    }
}
