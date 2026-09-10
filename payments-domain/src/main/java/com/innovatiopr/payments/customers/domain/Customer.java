package com.innovatiopr.payments.customers.domain;

import com.innovatiopr.payments.customers.CustomerId;
import com.innovatiopr.payments.shared.domain.AggregateRoot;
import com.innovatiopr.payments.shared.domain.Result;

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
     * Registers a new customer. Returns a {@link Result} rather than throwing, because invalid input is an
     * expected outcome of a public API, not a bug.
     */
    public static Result<Customer> register(CustomerId id, String firstName, String lastName, String email, Instant now) {
        Result<PersonName> name = PersonName.create(firstName, lastName);
        if (name.isFailure()) {
            return name.propagate();
        }
        Result<EmailAddress> address = EmailAddress.create(email);
        if (address.isFailure()) {
            return address.propagate();
        }
        Customer customer = new Customer(id, name.orElseThrow(), address.orElseThrow(), now, 0L);
        customer.raise(CustomerRegistered.of(customer, now));
        return Result.success(customer);
    }

    /**
     * Rebuilds an aggregate from stored state. Called only by the persistence mapper; it performs no
     * validation and raises no events, because the facts it replays already happened.
     */
    public static Customer reconstitute(CustomerId id, PersonName name, EmailAddress email, Instant registeredAt, long version) {
        return new Customer(id, name, email, registeredAt, version);
    }

    public Result<Void> changeEmail(String newEmail) {
        Result<EmailAddress> address = EmailAddress.create(newEmail);
        if (address.isFailure()) {
            return address.propagate();
        }
        this.email = address.orElseThrow();
        return Result.ok();
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
