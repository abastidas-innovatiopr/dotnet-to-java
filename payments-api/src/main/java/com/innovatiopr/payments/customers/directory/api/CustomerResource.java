package com.innovatiopr.payments.customers.directory.api;

import org.springframework.hateoas.RepresentationModel;

import java.time.Instant;
import java.util.UUID;

/**
 * Hypermedia representation of a customer.
 *
 * <p>Extends {@code RepresentationModel} so Spring HATEOAS can attach {@code _links}. Note that this is
 * neither the aggregate nor the JPA entity: exposing either would publish internal structure as the API
 * contract and make every refactor a breaking change.
 */
public class CustomerResource extends RepresentationModel<CustomerResource> {

    private final UUID id;
    private final String firstName;
    private final String lastName;
    private final String email;
    private final Instant registeredAt;
    private final Long accountCount;

    public CustomerResource(UUID id, String firstName, String lastName, String email, Instant registeredAt,
                            Long accountCount) {
        this.id = id;
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.registeredAt = registeredAt;
        this.accountCount = accountCount;
    }

    public UUID getId() {
        return id;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public String getFullName() {
        return firstName + " " + lastName;
    }

    public String getEmail() {
        return email;
    }

    public Instant getRegisteredAt() {
        return registeredAt;
    }

    public Long getAccountCount() {
        return accountCount;
    }
}
