package com.innovatiopr.payments.ledger.domain;

import com.innovatiopr.payments.ledger.LedgerTransactionId;
import com.innovatiopr.payments.ledger.PostingReference;
import com.innovatiopr.payments.shared.domain.DomainEvent;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Fact: a balanced set of postings was written to the ledger.
 *
 * <p>A business-process fact rather than an aggregate-local one — it is the ledger's statement that an
 * operation is now permanently recorded, and it is what an audit or reconciliation subscriber listens for.
 */
public record LedgerTransactionRecorded(UUID eventId, Instant occurredAt, String aggregateId,
                                        UUID postingReference, BigDecimal totalAmount, String currency,
                                        int entryCount) implements DomainEvent {

    static LedgerTransactionRecorded of(LedgerTransactionId id, PostingReference reference,
                                        BigDecimal totalAmount, String currency, int entryCount, Instant at) {
        return new LedgerTransactionRecorded(UUID.randomUUID(), at, id.toString(), reference.value(),
                totalAmount, currency, entryCount);
    }
}
