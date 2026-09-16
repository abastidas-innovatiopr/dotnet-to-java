package com.innovatiopr.payments.customers;

import com.innovatiopr.payments.shared.domain.NotFoundException;

/**
 * The Customers module's published contract.
 *
 * <p>Deliberately narrow. Note what is absent: the {@code Customer} aggregate, its repository and its
 * error factory. Another module that could construct a customer error would be deciding, on this
 * module's behalf, what counts as a customer problem — so {@link #requireExists} throws the module's own
 * exception instead. The caller never names the concrete error type, and the module that owns the rule
 * remains the module that states it.
 */
public interface CustomersApi {

    boolean exists(CustomerId customerId);

    /**
     * Returns normally when the customer exists, and otherwise throws this module's own "not found" error.
     *
     * @throws NotFoundException when no customer has that id
     */
    void requireExists(CustomerId customerId);

    /** Registers a customer. Published because it is one of this module's primary use cases. */
    CustomerId register(String firstName, String lastName, String email);
}
