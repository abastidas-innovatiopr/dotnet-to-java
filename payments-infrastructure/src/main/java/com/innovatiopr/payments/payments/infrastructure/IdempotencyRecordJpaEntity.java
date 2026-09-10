package com.innovatiopr.payments.payments.infrastructure;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/**
 * Persistence representation of an idempotency record.
 *
 * <p>No {@code @Version}: the row is written exactly once and never updated. Its primary key is the
 * idempotency key itself, which is what makes the unique constraint the concurrency control.
 */
@Entity
@Table(name = "idempotency_records")
class IdempotencyRecordJpaEntity {

    @Id
    @Column(name = "idempotency_key", nullable = false, updatable = false, length = 255)
    private String idempotencyKey;

    @Column(name = "request_hash", nullable = false, updatable = false, length = 64)
    private String requestHash;

    @Column(name = "transaction_id", nullable = false, updatable = false)
    private UUID transactionId;

    @Column(name = "response_status", nullable = false, updatable = false)
    private int responseStatus;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected IdempotencyRecordJpaEntity() {
        // required by Hibernate
    }

    IdempotencyRecordJpaEntity(String idempotencyKey, String requestHash, UUID transactionId,
                               int responseStatus, Instant createdAt) {
        this.idempotencyKey = idempotencyKey;
        this.requestHash = requestHash;
        this.transactionId = transactionId;
        this.responseStatus = responseStatus;
        this.createdAt = createdAt;
    }

    String getIdempotencyKey() {
        return idempotencyKey;
    }

    String getRequestHash() {
        return requestHash;
    }

    UUID getTransactionId() {
        return transactionId;
    }

    int getResponseStatus() {
        return responseStatus;
    }

    Instant getCreatedAt() {
        return createdAt;
    }
}
