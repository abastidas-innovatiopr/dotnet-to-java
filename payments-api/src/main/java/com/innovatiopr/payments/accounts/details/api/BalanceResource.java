package com.innovatiopr.payments.accounts.details.api;

import org.springframework.hateoas.RepresentationModel;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public class BalanceResource extends RepresentationModel<BalanceResource> {

    private final UUID accountId;
    private final String currency;
    private final BigDecimal balance;
    private final String status;
    private final Instant asOf;

    public BalanceResource(UUID accountId, String currency, BigDecimal balance, String status, Instant asOf) {
        this.accountId = accountId;
        this.currency = currency;
        this.balance = balance;
        this.status = status;
        this.asOf = asOf;
    }

    public UUID getAccountId() {
        return accountId;
    }

    public String getCurrency() {
        return currency;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public String getStatus() {
        return status;
    }

    public Instant getAsOf() {
        return asOf;
    }
}
