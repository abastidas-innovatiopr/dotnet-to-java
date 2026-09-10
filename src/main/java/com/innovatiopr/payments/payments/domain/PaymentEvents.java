package com.innovatiopr.payments.payments.domain;

import com.innovatiopr.payments.shared.domain.DomainEvent;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Facts published by the {@code PaymentTransaction} aggregate.
 *
 * <p>These are <em>business-process</em> facts. Where {@code MoneyDebited} says "this account's balance
 * fell", {@code TransferCompleted} says "this business operation finished successfully" — one event for
 * the whole operation, carrying both accounts. Subscribers that care about the operation (notifications,
 * fraud scoring, reconciliation) bind here; subscribers that care about a single balance bind to the
 * account events instead.
 */
public final class PaymentEvents {

    private PaymentEvents() {
    }

    public record TransferInitiated(UUID eventId, Instant occurredAt, String aggregateId, String sourceAccountId,
                                    String destinationAccountId, BigDecimal amount, String currency)
            implements DomainEvent { }

    public record TransferCompleted(UUID eventId, Instant occurredAt, String aggregateId, String sourceAccountId,
                                    String destinationAccountId, BigDecimal amount, String currency,
                                    String reference) implements DomainEvent { }

    public record TransferFailed(UUID eventId, Instant occurredAt, String aggregateId, String sourceAccountId,
                                 String destinationAccountId, BigDecimal amount, String currency,
                                 String failureCode) implements DomainEvent { }

    public record DepositRecorded(UUID eventId, Instant occurredAt, String aggregateId, String accountId,
                                  BigDecimal amount, String currency) implements DomainEvent { }

    public record WithdrawalRecorded(UUID eventId, Instant occurredAt, String aggregateId, String accountId,
                                     BigDecimal amount, String currency) implements DomainEvent { }
}
