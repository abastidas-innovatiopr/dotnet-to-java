package com.innovatiopr.payments.customers.directory.application;

import com.innovatiopr.payments.customers.CustomerId;
import com.innovatiopr.payments.shared.application.PageRequest;
import com.innovatiopr.payments.shared.application.PageResult;

import java.util.Optional;

/**
 * Read-side port for customers.
 *
 * <p>Separate from {@code CustomerRepository}: that one loads aggregates for writing, this one projects
 * rows for reading. Keeping them apart is what lets the query side add a join, a computed column or an
 * index without any pressure on the domain model.
 */
public interface CustomerReadModel {

    Optional<CustomerDetails> findById(CustomerId id);

    PageResult<CustomerSummary> list(PageRequest request);
}
