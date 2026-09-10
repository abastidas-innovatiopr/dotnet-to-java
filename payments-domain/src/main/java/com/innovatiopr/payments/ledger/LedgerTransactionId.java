package com.innovatiopr.payments.ledger;

import java.util.Objects;
import java.util.UUID;

/** Identity of a {@code LedgerTransaction} — one balanced set of double-entry postings. */
public record LedgerTransactionId(UUID value) {

    public LedgerTransactionId {
        Objects.requireNonNull(value, "value");
    }

    public static LedgerTransactionId generate() {
        return new LedgerTransactionId(UUID.randomUUID());
    }

    public static LedgerTransactionId of(UUID value) {
        return new LedgerTransactionId(value);
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
