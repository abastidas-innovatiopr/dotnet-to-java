package com.innovatiopr.payments.shared.domain;

import java.util.Objects;

/**
 * The request conflicts with state that already exists — a duplicate natural key, or a reused
 * idempotency key whose stored request differs from this one. Surfaces as HTTP 409.
 *
 * <p>Distinct from {@link DomainException}: nothing about the request is wrong in itself, it simply
 * cannot be reconciled with what is already stored. Retrying it unchanged will fail the same way.
 *
 * <p>Lives in the domain module for the reason given on {@link NotFoundException}.
 */
public final class ConflictException extends RuntimeException {

    private final String code;

    public ConflictException(String code, String message) {
        super(Objects.requireNonNull(message, "message"));
        this.code = Objects.requireNonNull(code, "code");
    }

    /** Mirrors the reference's {@code ConflictException.For(name, fieldName, value)}. */
    public static ConflictException of(String code, String entityName, String fieldName, Object value) {
        return new ConflictException(
                code,
                "A %s with %s '%s' already exists".formatted(entityName, fieldName, value));
    }

    public String code() {
        return code;
    }
}
