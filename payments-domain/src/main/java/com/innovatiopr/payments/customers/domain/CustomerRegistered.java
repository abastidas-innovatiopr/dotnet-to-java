package com.innovatiopr.payments.customers.domain;

import com.innovatiopr.payments.shared.domain.DomainEvent;

import java.time.Instant;
import java.util.UUID;

/** Fact: a customer was registered. Aggregate-local. */
public record CustomerRegistered(UUID eventId, Instant occurredAt, String aggregateId, String email, String fullName)
        implements DomainEvent {

    public static CustomerRegistered of(Customer customer, Instant occurredAt) {
        return new CustomerRegistered(
                UUID.randomUUID(),
                occurredAt,
                customer.id().toString(),
                customer.email().value(),
                customer.name().fullName());
    }
}
