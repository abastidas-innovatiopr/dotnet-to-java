package com.innovatiopr.payments.payments.deposits.api;

import org.springframework.hateoas.RepresentationModel;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public class CashMovementResource extends RepresentationModel<CashMovementResource> {

    private final UUID transactionId;
    private final UUID accountId;
    private final BigDecimal amount;
    private final String currency;
    private final String status;
    private final BigDecimal balanceAfter;
    private final Instant completedAt;

    public CashMovementResource(UUID transactionId, UUID accountId, BigDecimal amount, String currency,
                                String status, BigDecimal balanceAfter, Instant completedAt) {
        this.transactionId = transactionId;
        this.accountId = accountId;
        this.amount = amount;
        this.currency = currency;
        this.status = status;
        this.balanceAfter = balanceAfter;
        this.completedAt = completedAt;
    }

    public UUID getTransactionId() {
        return transactionId;
    }

    public UUID getAccountId() {
        return accountId;
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

    public BigDecimal getBalanceAfter() {
        return balanceAfter;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }
}
