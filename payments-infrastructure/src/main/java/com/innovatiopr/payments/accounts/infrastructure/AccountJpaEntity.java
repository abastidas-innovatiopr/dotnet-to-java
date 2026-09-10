package com.innovatiopr.payments.accounts.infrastructure;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/** Persistence representation of an account. */
@Entity
@Table(name = "accounts")
class AccountJpaEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "customer_id", nullable = false, updatable = false)
    private UUID customerId;

    @Column(name = "account_number", nullable = false, updatable = false, length = 12)
    private String accountNumber;

    @Column(name = "currency", nullable = false, updatable = false, length = 3)
    private String currency;

    /**
     * Precision and scale must match {@code NUMERIC(19, 4)} in the migration, or
     * {@code hibernate.ddl-auto=validate} fails at startup — which is the point: a silent mismatch here
     * would mean rounding behaviour that differs between the application and the database.
     */
    @Column(name = "balance", nullable = false, precision = 19, scale = 4)
    private BigDecimal balance;

    @Column(name = "status", nullable = false, length = 16)
    private String status;

    @Column(name = "opened_at", nullable = false, updatable = false)
    private Instant openedAt;

    @Version
    @Column(name = "version", nullable = false)
    private long version;

    protected AccountJpaEntity() {
        // required by Hibernate
    }

    AccountJpaEntity(UUID id, UUID customerId, String accountNumber, String currency, BigDecimal balance,
                     String status, Instant openedAt, long version) {
        this.id = id;
        this.customerId = customerId;
        this.accountNumber = accountNumber;
        this.currency = currency;
        this.balance = balance;
        this.status = status;
        this.openedAt = openedAt;
        this.version = version;
    }

    UUID getId() {
        return id;
    }

    UUID getCustomerId() {
        return customerId;
    }

    String getAccountNumber() {
        return accountNumber;
    }

    String getCurrency() {
        return currency;
    }

    BigDecimal getBalance() {
        return balance;
    }

    void setBalance(BigDecimal balance) {
        this.balance = balance;
    }

    String getStatus() {
        return status;
    }

    void setStatus(String status) {
        this.status = status;
    }

    Instant getOpenedAt() {
        return openedAt;
    }

    long getVersion() {
        return version;
    }
}
