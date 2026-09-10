package com.innovatiopr.payments.ledger.statements.api;

import org.springframework.hateoas.RepresentationModel;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public class StatementLineResource extends RepresentationModel<StatementLineResource> {

    private final UUID entryId;
    private final UUID ledgerTransactionId;
    private final UUID transactionId;
    private final String direction;
    private final BigDecimal amount;
    private final String currency;
    private final BigDecimal runningBalance;
    private final String description;
    private final Instant recordedAt;

    public StatementLineResource(UUID entryId, UUID ledgerTransactionId, UUID transactionId, String direction,
                                 BigDecimal amount, String currency, BigDecimal runningBalance,
                                 String description, Instant recordedAt) {
        this.entryId = entryId;
        this.ledgerTransactionId = ledgerTransactionId;
        this.transactionId = transactionId;
        this.direction = direction;
        this.amount = amount;
        this.currency = currency;
        this.runningBalance = runningBalance;
        this.description = description;
        this.recordedAt = recordedAt;
    }

    public UUID getEntryId() {
        return entryId;
    }

    public UUID getLedgerTransactionId() {
        return ledgerTransactionId;
    }

    public UUID getTransactionId() {
        return transactionId;
    }

    public String getDirection() {
        return direction;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public String getCurrency() {
        return currency;
    }

    public BigDecimal getRunningBalance() {
        return runningBalance;
    }

    public String getDescription() {
        return description;
    }

    public Instant getRecordedAt() {
        return recordedAt;
    }
}
