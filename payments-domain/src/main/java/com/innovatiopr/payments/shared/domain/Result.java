package com.innovatiopr.payments.shared.domain;

import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * An {@code ErrorOr}-style discriminated union: either a value, or one or more {@link DomainError}s.
 *
 * <p>This is the return type for every <em>expected</em> business outcome. Exceptions remain reserved for
 * programming bugs and infrastructure failures. The .NET equivalent is {@code ErrorOr<T>}; the difference
 * is that Java models the union with a {@code sealed interface} plus records, which gives exhaustive
 * pattern matching in {@code switch}.
 *
 * <p>Sealing works here — unlike {@link DomainError} — because the two permitted implementations are
 * nested types, and therefore live in the same package as the interface.
 */
public sealed interface Result<T> permits Result.Success, Result.Failure {

    record Success<T>(T value) implements Result<T> { }

    record Failure<T>(List<DomainError> errors) implements Result<T> {

        public Failure {
            Objects.requireNonNull(errors, "errors");
            if (errors.isEmpty()) {
                throw new IllegalArgumentException("A failed Result must carry at least one DomainError");
            }
            errors = List.copyOf(errors);
        }

        /**
         * Re-types this failure for a different payload. Safe because a failure holds no {@code T},
         * so no value of the old type can escape.
         */
        <R> Result<R> retype() {
            return new Failure<>(errors);
        }
    }

    static <T> Result<T> success(T value) {
        return new Success<>(value);
    }

    /** A successful result carrying no payload — the equivalent of {@code ErrorOr<Success>}. */
    static Result<Void> ok() {
        return new Success<>(null);
    }

    static <T> Result<T> failure(DomainError error) {
        return new Failure<>(List.of(Objects.requireNonNull(error, "error")));
    }

    static <T> Result<T> failure(List<DomainError> errors) {
        return new Failure<>(errors);
    }

    default boolean isSuccess() {
        return this instanceof Success<T>;
    }

    default boolean isFailure() {
        return this instanceof Failure<T>;
    }

    /** Errors carried by this result; empty when successful ({@code Failure} overrides via its accessor). */
    default List<DomainError> errors() {
        return List.of();
    }

    default DomainError firstError() {
        List<DomainError> errors = errors();
        if (errors.isEmpty()) {
            throw new IllegalStateException("A successful Result has no errors");
        }
        return errors.getFirst();
    }

    /**
     * Propagates this failure as a differently-typed {@code Result}. Use when a handler must abort early
     * and hand the caller's error list upward unchanged.
     */
    default <R> Result<R> propagate() {
        return switch (this) {
            case Success<T> ignored ->
                    throw new IllegalStateException("Cannot propagate a successful Result as a failure");
            case Failure<T> failure -> failure.retype();
        };
    }

    /** Unwraps the value. Only call once the result is known to be successful. */
    default T orElseThrow() {
        return switch (this) {
            case Success<T>(T value) -> value;
            case Failure<T> failure ->
                    throw new IllegalStateException("Result failed with " + failure.errors().getFirst().code());
        };
    }

    default <R> Result<R> map(Function<? super T, ? extends R> mapper) {
        return switch (this) {
            case Success<T>(T value) -> Result.success(mapper.apply(value));
            case Failure<T> failure -> failure.retype();
        };
    }

    default <R> Result<R> flatMap(Function<? super T, Result<R>> mapper) {
        return switch (this) {
            case Success<T>(T value) -> mapper.apply(value);
            case Failure<T> failure -> failure.retype();
        };
    }

    default <R> R fold(Function<? super T, ? extends R> onSuccess,
                       Function<List<DomainError>, ? extends R> onFailure) {
        return switch (this) {
            case Success<T>(T value) -> onSuccess.apply(value);
            case Failure<T> failure -> onFailure.apply(failure.errors());
        };
    }

    default Result<T> onSuccess(Consumer<? super T> action) {
        if (this instanceof Success<T>(T value)) {
            action.accept(value);
        }
        return this;
    }

    default Result<T> onFailure(Consumer<List<DomainError>> action) {
        if (this instanceof Failure<T> failure) {
            action.accept(failure.errors());
        }
        return this;
    }
}
