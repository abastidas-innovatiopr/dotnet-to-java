package com.innovatiopr.payments.customers.domain;

import com.innovatiopr.payments.customers.CustomerId;
import com.innovatiopr.payments.shared.domain.AggregateRoot;

import java.time.Instant;
import java.util.Objects;

/**
 * The Customer aggregate root.
 *
 * <p>Note what is absent: no {@code @Entity}, no {@code @Column}, no setters. Persistence is handled by a
 * separate {@code CustomerJpaEntity} in the infrastructure layer, and state changes happen only through
 * intention-revealing methods that enforce invariants and record events.
 */
public final class Customer extends AggregateRoot<CustomerId> {

    private final CustomerId id;
    private PersonName name;
    private EmailAddress email;
    private final Instant registeredAt;
    private long version;

    private Customer(CustomerId id, PersonName name, EmailAddress email, Instant registeredAt, long version) {
        this.id = Objects.requireNonNull(id, "id");
        this.name = Objects.requireNonNull(name, "name");
        this.email = Objects.requireNonNull(email, "email");
        this.registeredAt = Objects.requireNonNull(registeredAt, "registeredAt");
        this.version = version;
    }

    /**
     * Registers a new customer. Invalid input throws a
     * {@link com.innovatiopr.payments.shared.domain.ValidationException} carrying the offending field's
     * code — the value objects refuse to be constructed, so an invalid {@code Customer} cannot exist.
     */
    public static Customer register(CustomerId id, String firstName, String lastName, String email, Instant now) {
        Customer customer = new Customer(
                id,
                PersonName.create(firstName, lastName),
                EmailAddress.create(email),
                now,
                0L);
        customer.raise(CustomerRegistered.of(customer, now));
        return customer;
    }

    /**
     * Rebuilds an aggregate from stored state. Called only by the persistence mapper; it performs no
     * validation and raises no events, because the facts it replays already happened.
     */
    public static Customer reconstitute(CustomerId id, PersonName name, EmailAddress email, Instant registeredAt, long version) {
        return new Customer(id, name, email, registeredAt, version);
    }

    public void changeEmail(String newEmail) {
        this.email = EmailAddress.create(newEmail);
    }

    @Override
    public CustomerId id() {
        return id;
    }

    public PersonName name() {
        return name;
    }

    public EmailAddress email() {
        return email;
    }

    public Instant registeredAt() {
        return registeredAt;
    }

    /** Optimistic-locking version. Carried on the aggregate so the mapper can round-trip it — see README. */
    public long version() {
        return version;
    }
}
