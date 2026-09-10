package com.innovatiopr.payments.customers.infrastructure;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.Instant;
import java.util.UUID;

/**
 * Persistence representation of a customer.
 *
 * <p>Deliberately separate from the {@code Customer} aggregate. Hibernate needs a no-arg constructor,
 * non-final fields and mutable setters so it can instantiate and populate rows reflectively — exactly the
 * shape a rich domain model must not have. Keeping the two apart means the aggregate can stay immutable
 * where it wants to be, and can be redesigned without a migration.
 *
 * <p>Package-private: nothing outside this package should ever hold a JPA entity, and an ArchUnit rule
 * checks that persistence entities stay inside infrastructure packages.
 */
@Entity
@Table(name = "customers")
class CustomerJpaEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "first_name", nullable = false, length = 100)
    private String firstName;

    @Column(name = "last_name", nullable = false, length = 100)
    private String lastName;

    @Column(name = "email", nullable = false, length = 254)
    private String email;

    @Column(name = "registered_at", nullable = false, updatable = false)
    private Instant registeredAt;

    /**
     * Optimistic-locking version.
     *
     * <p>Hibernate increments this on every update and adds {@code WHERE version = ?} to the statement, so
     * a write based on stale data fails with an {@code OptimisticLockException} instead of silently
     * overwriting a concurrent change.
     *
     * <p>{@code save} re-reads the managed entity and copies the aggregate's state onto it, so the version
     * held here is always the one Hibernate loaded and the check works without further help. The aggregate
     * still carries its version — it is part of the aggregate's observable state, and any future move to a
     * detached-merge strategy would depend on it round-tripping rather than being invented at write time.
     */
    @Version
    @Column(name = "version", nullable = false)
    private long version;

    protected CustomerJpaEntity() {
        // required by Hibernate
    }

    CustomerJpaEntity(UUID id, String firstName, String lastName, String email, Instant registeredAt, long version) {
        this.id = id;
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.registeredAt = registeredAt;
        this.version = version;
    }

    UUID getId() {
        return id;
    }

    String getFirstName() {
        return firstName;
    }

    void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    String getLastName() {
        return lastName;
    }

    void setLastName(String lastName) {
        this.lastName = lastName;
    }

    String getEmail() {
        return email;
    }

    void setEmail(String email) {
        this.email = email;
    }

    Instant getRegisteredAt() {
        return registeredAt;
    }

    long getVersion() {
        return version;
    }
}
