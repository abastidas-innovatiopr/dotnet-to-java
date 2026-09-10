package com.innovatiopr.payments.accounts.details.api;

import org.springframework.hateoas.RepresentationModel;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public class AccountResource extends RepresentationModel<AccountResource> {

    private final UUID id;
    private final UUID customerId;
    private final String accountNumber;
    private final String currency;
    private final BigDecimal balance;
    private final String status;
    private final Instant openedAt;

    public AccountResource(UUID id, UUID customerId, String accountNumber, String currency, BigDecimal balance,
                           String status, Instant openedAt) {
        this.id = id;
        this.customerId = customerId;
        this.accountNumber = accountNumber;
        this.currency = currency;
        this.balance = balance;
        this.status = status;
        this.openedAt = openedAt;
    }

    public UUID getId() {
        return id;
    }

    public UUID getCustomerId() {
        return customerId;
    }

    public String getAccountNumber() {
        return accountNumber;
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

    public Instant getOpenedAt() {
        return openedAt;
    }
}
