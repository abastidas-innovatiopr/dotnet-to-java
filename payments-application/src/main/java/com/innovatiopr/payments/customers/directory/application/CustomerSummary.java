package com.innovatiopr.payments.customers.directory.application;

import java.time.Instant;
import java.util.UUID;

/**
 * Read model for a row in the customer list.
 *
 * <p>Carries {@code accountCount}, which the write model does not hold anywhere: the Customer aggregate
 * knows nothing about accounts. Computing it needs a join the aggregate would never perform, which is
 * precisely why the read side is allowed its own shape.
 */
public record CustomerSummary(UUID id, String fullName, String email, Instant registeredAt, long accountCount) { }
