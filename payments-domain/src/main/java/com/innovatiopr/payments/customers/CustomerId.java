package com.innovatiopr.payments.customers;

import java.util.Objects;
import java.util.UUID;

/**
 * Identity of a {@code Customer} aggregate.
 *
 * <p>Lives in the module's root package because other modules (Accounts) legitimately need to reference a
 * customer. Spring Modulith treats the root package as the module's published API and every sub-package
 * as internal, so {@code accounts} can use {@code CustomerId} but cannot reach
 * {@code customers.domain.Customer}.
 *
 * <p>A wrapper rather than a bare {@code UUID}: it makes {@code openAccount(customerId, accountId)}
 * impossible to call with the arguments swapped, which is a mistake the compiler cannot catch when both
 * parameters are {@code UUID}.
 */
public record CustomerId(UUID value) {

    public CustomerId {
        Objects.requireNonNull(value, "value");
    }

    public static CustomerId generate() {
        return new CustomerId(UUID.randomUUID());
    }

    public static CustomerId of(UUID value) {
        return new CustomerId(value);
    }

    /** @throws IllegalArgumentException when {@code raw} is not a valid UUID. */
    public static CustomerId parse(String raw) {
        return new CustomerId(UUID.fromString(raw));
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
