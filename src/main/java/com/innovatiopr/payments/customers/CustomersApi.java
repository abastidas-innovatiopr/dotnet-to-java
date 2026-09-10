package com.innovatiopr.payments.customers;

import com.innovatiopr.payments.shared.domain.Result;

/**
 * The Customers module's published contract.
 *
 * <p>Deliberately narrow. Note what is absent: the {@code Customer} aggregate, its repository and its
 * error types. Another module that could construct a {@code CustomerError} would be deciding, on this
 * module's behalf, what counts as a customer problem — so {@link #requireExists} returns the error
 * <em>inside</em> a {@code Result} instead. The caller propagates a {@code DomainError} it never names,
 * and the module that owns the rule remains the module that states it.
 */
public interface CustomersApi {

    boolean exists(CustomerId customerId);

    /**
     * Succeeds when the customer exists, and otherwise fails with this module's own "not found" error.
     *
     * <p>This is the reason callers do not import {@code CustomerError}: they ask the question and
     * propagate whatever answer comes back.
     */
    Result<Void> requireExists(CustomerId customerId);

    /** Registers a customer. Published because it is one of this module's primary use cases. */
    Result<CustomerId> register(String firstName, String lastName, String email);
}
