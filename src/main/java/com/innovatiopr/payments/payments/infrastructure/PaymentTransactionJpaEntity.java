package com.innovatiopr.payments.payments.infrastructure;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/** Persistence representation of a payment transaction. */
@Entity
@Table(name = "payment_transactions")
class PaymentTransactionJpaEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "type", nullable = false, updatable = false, length = 16)
    private String type;

    @Column(name = "source_account_id", updatable = false)
    private UUID sourceAccountId;

    @Column(name = "destination_account_id", updatable = false)
    private UUID destinationAccountId;

    @Column(name = "amount", nullable = false, updatable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    @Column(name = "currency", nullable = false, updatable = false, length = 3)
    private String currency;

    @Column(name = "status", nullable = false, length = 16)
    private String status;

    @Column(name = "reference", nullable = false, length = 140)
    private String reference;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "failure_code", length = 64)
    private String failureCode;

    @Version
    @Column(name = "version", nullable = false)
    private long version;

    protected PaymentTransactionJpaEntity() {
        // required by Hibernate
    }

    PaymentTransactionJpaEntity(UUID id, String type, UUID sourceAccountId, UUID destinationAccountId,
                                BigDecimal amount, String currency, String status, String reference,
                                Instant createdAt, Instant completedAt, String failureCode) {
        this.id = id;
        this.type = type;
        this.sourceAccountId = sourceAccountId;
        this.destinationAccountId = destinationAccountId;
        this.amount = amount;
        this.currency = currency;
        this.status = status;
        this.reference = reference;
        this.createdAt = createdAt;
        this.completedAt = completedAt;
        this.failureCode = failureCode;
    }

    UUID getId() {
        return id;
    }

    String getType() {
        return type;
    }

    UUID getSourceAccountId() {
        return sourceAccountId;
    }

    UUID getDestinationAccountId() {
        return destinationAccountId;
    }

    BigDecimal getAmount() {
        return amount;
    }

    String getCurrency() {
        return currency;
    }

    String getStatus() {
        return status;
    }

    void setStatus(String status) {
        this.status = status;
    }

    String getReference() {
        return reference;
    }

    void setReference(String reference) {
        this.reference = reference;
    }

    Instant getCreatedAt() {
        return createdAt;
    }

    Instant getCompletedAt() {
        return completedAt;
    }

    void setCompletedAt(Instant completedAt) {
        this.completedAt = completedAt;
    }

    String getFailureCode() {
        return failureCode;
    }

    void setFailureCode(String failureCode) {
        this.failureCode = failureCode;
    }
}
