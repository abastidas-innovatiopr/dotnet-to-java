package com.innovatiopr.payments.shared.domain;

import java.util.Objects;

/**
 * The caller is authenticated but not permitted to act on this resource. Surfaces as HTTP 403.
 *
 * <p>Nothing throws this yet — the application has no authentication layer. It exists so the error
 * model is complete and the {@code ApiExceptionHandler} mapping mirrors the reference's
 * {@code GlobalExceptionHandler} in full. When authorization arrives it belongs in the application
 * layer, checked against the caller identity rather than anything on the request.
 */
public final class ForbiddenException extends RuntimeException {

    private final String code;

    public ForbiddenException(String code, String message) {
        super(Objects.requireNonNull(message, "message"));
        this.code = Objects.requireNonNull(code, "code");
    }

    public static ForbiddenException of(String entityName, Object key) {
        return new ForbiddenException(
                "FORBIDDEN",
                "The caller may not act on %s '%s'".formatted(entityName, key));
    }

    public String code() {
        return code;
    }
}
