package com.innovatiopr.payments.payments.transactions.api;

import org.springframework.hateoas.RepresentationModel;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public class TransactionResource extends RepresentationModel<TransactionResource> {

    private final UUID id;
    private final String type;
    private final UUID sourceAccountId;
    private final UUID destinationAccountId;
    private final BigDecimal amount;
    private final String currency;
    private final String status;
    private final String reference;
    private final Instant createdAt;
    private final Instant completedAt;
    private final String failureCode;

    public TransactionResource(UUID id, String type, UUID sourceAccountId, UUID destinationAccountId,
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

    public UUID getId() {
        return id;
    }

    public String getType() {
        return type;
    }

    public UUID getSourceAccountId() {
        return sourceAccountId;
    }

    public UUID getDestinationAccountId() {
        return destinationAccountId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public String getCurrency() {
        return currency;
    }

    public String getStatus() {
        return status;
    }

    public String getReference() {
        return reference;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public String getFailureCode() {
        return failureCode;
    }
}
