package com.innovatiopr.payments.shared.domain;

import java.util.Objects;

/**
 * Input could not be accepted at all — malformed, out of range, or structurally invalid. Surfaces as
 * HTTP 400.
 *
 * <h2>Why this is separate from {@link DomainException}</h2>
 * The distinction is the one the old {@code ErrorType} enum drew between {@code VALIDATION} and
 * {@code BUSINESS_RULE}, and it is worth keeping: a malformed account number is a 400 because the
 * request could never be accepted in any state of the world, whereas insufficient funds is a 422
 * because the request is perfectly well formed and the domain simply refused it today. Collapsing
 * both to 400 would tell a client to fix its request when there is nothing to fix.
 *
 * <p>The reference's {@code GlobalExceptionHandler} draws the same line, mapping FluentValidation's
 * {@code ValidationException} to 400 alongside {@code DomainException}.
 *
 * <p>This covers validation the <em>domain</em> performs on values it is handed. Bean-Validation
 * failures on a request body never reach here — Spring raises {@code MethodArgumentNotValidException}
 * before a handler runs, and the API layer maps that separately so it can report per-field errors.
 */
public final class ValidationException extends RuntimeException {

    private final String code;

    public ValidationException(String code, String message) {
        super(Objects.requireNonNull(message, "message"));
        this.code = Objects.requireNonNull(code, "code");
    }

    public String code() {
        return code;
    }
}
