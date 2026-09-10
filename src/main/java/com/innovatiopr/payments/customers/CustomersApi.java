package com.innovatiopr.payments.customers;

/**
 * The Customers module's published contract.
 *
 * <p>Deliberately narrow: Accounts needs to know that a customer exists before opening an account for
 * them, and nothing more. Exposing the {@code Customer} aggregate itself would let another module mutate
 * state whose invariants this module owns.
 */
public interface CustomersApi {

    boolean exists(CustomerId customerId);
}
