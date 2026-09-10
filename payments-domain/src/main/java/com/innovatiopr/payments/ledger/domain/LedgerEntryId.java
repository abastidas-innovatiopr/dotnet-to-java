package com.innovatiopr.payments.ledger.domain;

import java.util.Objects;
import java.util.UUID;

public record LedgerEntryId(UUID value) {

    public LedgerEntryId {
        Objects.requireNonNull(value, "value");
    }

    public static LedgerEntryId generate() {
        return new LedgerEntryId(UUID.randomUUID());
    }

    public static LedgerEntryId of(UUID value) {
        return new LedgerEntryId(value);
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
