package com.innovatiopr.payments.customers.directory.application;

import java.time.Instant;
import java.util.UUID;

/** Read model for a single customer. Built by {@code JdbcClient}, never by loading the aggregate. */
public record CustomerDetails(UUID id, String firstName, String lastName, String email, Instant registeredAt) {

    public String fullName() {
        return firstName + " " + lastName;
    }
}
