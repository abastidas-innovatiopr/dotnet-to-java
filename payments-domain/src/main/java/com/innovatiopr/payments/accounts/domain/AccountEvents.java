package com.innovatiopr.payments.accounts.domain;

import com.innovatiopr.payments.accounts.AccountId;
import com.innovatiopr.payments.shared.domain.DomainEvent;
import com.innovatiopr.payments.shared.domain.Money;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Facts published by the Account aggregate.
 *
 * <h2>Event granularity — a deliberate decision</h2>
 * These are all <em>aggregate-local</em> facts: "this account's balance changed, for this reason". They say
 * nothing about why the money moved, because the account does not know. The <em>business-process</em> facts
 * ({@code TransferCompleted}, {@code TransferFailed}) belong to the Payments module, and
 * {@code LedgerTransactionRecorded} belongs to Ledger.
 *
 * <p>The distinction matters when choosing subscribers. A fraud monitor wants
 * {@code TransferCompleted} — one event per business operation. A balance-change audit wants
 * {@code MoneyDebited}/{@code MoneyCredited} — one event per affected account. Publishing only the
 * fine-grained events would force every consumer to re-assemble the business operation; publishing only
 * the coarse one would hide deposits and withdrawals. Both levels exist, and neither is redundant.
 *
 * <p>Deposits and withdrawals raise {@code MoneyDeposited}/{@code MoneyWithdrawn} rather than
 * {@code MoneyCredited}/{@code MoneyDebited}: the customer-initiated cash operations are a different fact
 * from an internal transfer posting, even though both change the balance the same way.
 *
 * <p>Amounts are carried as {@code BigDecimal} plus an ISO currency code rather than as {@code Money},
 * because events are serialised into the Modulith publication log and a flat shape stays readable and
 * stable across refactors of the value object.
 */
public final class AccountEvents {

    private AccountEvents() {
    }

    public record AccountOpened(UUID eventId, Instant occurredAt, String aggregateId, String customerId,
                                String accountNumber, String currency) implements DomainEvent { }

    public record MoneyDeposited(UUID eventId, Instant occurredAt, String aggregateId, BigDecimal amount,
                                 String currency, BigDecimal resultingBalance) implements DomainEvent { }

    public record MoneyWithdrawn(UUID eventId, Instant occurredAt, String aggregateId, BigDecimal amount,
                                 String currency, BigDecimal resultingBalance) implements DomainEvent { }

    public record MoneyDebited(UUID eventId, Instant occurredAt, String aggregateId, BigDecimal amount,
                               String currency, BigDecimal resultingBalance) implements DomainEvent { }

    public record MoneyCredited(UUID eventId, Instant occurredAt, String aggregateId, BigDecimal amount,
                                String currency, BigDecimal resultingBalance) implements DomainEvent { }

    public record AccountFrozen(UUID eventId, Instant occurredAt, String aggregateId) implements DomainEvent { }

    public record AccountUnfrozen(UUID eventId, Instant occurredAt, String aggregateId) implements DomainEvent { }

    public record AccountClosed(UUID eventId, Instant occurredAt, String aggregateId) implements DomainEvent { }

    static AccountOpened opened(AccountId id, String customerId, AccountNumber number,
                                java.util.Currency currency, Instant at) {
        return new AccountOpened(UUID.randomUUID(), at, id.toString(), customerId, number.value(),
                currency.getCurrencyCode());
    }

    static MoneyDeposited deposited(AccountId id, Money amount, Money balance, Instant at) {
        return new MoneyDeposited(UUID.randomUUID(), at, id.toString(), amount.amount(),
                amount.currency().getCurrencyCode(), balance.amount());
    }

    static MoneyWithdrawn withdrawn(AccountId id, Money amount, Money balance, Instant at) {
        return new MoneyWithdrawn(UUID.randomUUID(), at, id.toString(), amount.amount(),
                amount.currency().getCurrencyCode(), balance.amount());
    }

    static MoneyDebited debited(AccountId id, Money amount, Money balance, Instant at) {
        return new MoneyDebited(UUID.randomUUID(), at, id.toString(), amount.amount(),
                amount.currency().getCurrencyCode(), balance.amount());
    }

    static MoneyCredited credited(AccountId id, Money amount, Money balance, Instant at) {
        return new MoneyCredited(UUID.randomUUID(), at, id.toString(), amount.amount(),
                amount.currency().getCurrencyCode(), balance.amount());
    }

    static AccountFrozen frozen(AccountId id, Instant at) {
        return new AccountFrozen(UUID.randomUUID(), at, id.toString());
    }

    static AccountUnfrozen unfrozen(AccountId id, Instant at) {
        return new AccountUnfrozen(UUID.randomUUID(), at, id.toString());
    }

    static AccountClosed closed(AccountId id, Instant at) {
        return new AccountClosed(UUID.randomUUID(), at, id.toString());
    }
}
