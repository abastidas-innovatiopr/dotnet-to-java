package com.innovatiopr.payments.payments.domain;

import com.innovatiopr.payments.shared.domain.Result;

import java.util.Objects;

/** Free-text description a customer attaches to a payment, for example "Rent payment". */
public record TransactionReference(String value) {

    public static final int MAX_LENGTH = 140;

    public TransactionReference {
        Objects.requireNonNull(value, "value");
    }

    public static Result<TransactionReference> create(String raw) {
        String normalised = raw == null ? "" : raw.trim();
        if (normalised.length() > MAX_LENGTH) {
            return Result.failure(TransferError.invalidReference(
                    "Reference must be at most " + MAX_LENGTH + " characters"));
        }
        return Result.success(new TransactionReference(normalised));
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
