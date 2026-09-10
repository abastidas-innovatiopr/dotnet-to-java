package com.innovatiopr.payments.customers.application;

import com.innovatiopr.payments.customers.CustomerId;
import com.innovatiopr.payments.customers.domain.Customer;
import com.innovatiopr.payments.customers.domain.EmailAddress;

import java.util.Optional;

/** The Customers module's persistence port. Domain-oriented, framework-free. */
public interface CustomerRepository {

    Optional<Customer> findById(CustomerId id);

    boolean existsById(CustomerId id);

    boolean existsByEmail(EmailAddress email);

    void save(Customer customer);
}
