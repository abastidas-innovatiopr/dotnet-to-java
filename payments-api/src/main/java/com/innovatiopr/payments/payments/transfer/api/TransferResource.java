package com.innovatiopr.payments.payments.transfer.api;

import org.springframework.hateoas.RepresentationModel;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public class TransferResource extends RepresentationModel<TransferResource> {

    private final UUID transactionId;
    private final UUID sourceAccountId;
    private final UUID destinationAccountId;
    private final BigDecimal amount;
    private final String currency;
    private final String status;
    private final String reference;
    private final BigDecimal sourceBalanceAfter;
    private final BigDecimal destinationBalanceAfter;
    private final Instant completedAt;
    private final boolean replayed;

    public TransferResource(UUID transactionId, UUID sourceAccountId, UUID destinationAccountId,
                            BigDecimal amount, String currency, String status, String reference,
                            BigDecimal sourceBalanceAfter, BigDecimal destinationBalanceAfter,
                            Instant completedAt, boolean replayed) {
        this.transactionId = transactionId;
        this.sourceAccountId = sourceAccountId;
        this.destinationAccountId = destinationAccountId;
        this.amount = amount;
        this.currency = currency;
        this.status = status;
        this.reference = reference;
        this.sourceBalanceAfter = sourceBalanceAfter;
        this.destinationBalanceAfter = destinationBalanceAfter;
        this.completedAt = completedAt;
        this.replayed = replayed;
    }

    public UUID getTransactionId() {
        return transactionId;
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

    public BigDecimal getSourceBalanceAfter() {
        return sourceBalanceAfter;
    }

    public BigDecimal getDestinationBalanceAfter() {
        return destinationBalanceAfter;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    /** True when this response was replayed from a stored idempotency record rather than moving money. */
    public boolean isReplayed() {
        return replayed;
    }
}
