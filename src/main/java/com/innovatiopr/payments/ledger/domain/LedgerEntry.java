package com.innovatiopr.payments.ledger.domain;

import com.innovatiopr.payments.shared.domain.Money;

import java.util.Objects;

/**
 * One side of a double-entry posting. An entity inside the {@code LedgerTransaction} aggregate — it has
 * identity, but it is never loaded, modified or deleted independently of its transaction.
 *
 * <p>{@code amount} is always positive; direction carries the sign. Storing signed amounts instead would
 * make "sum to zero" the invariant, which is easy to satisfy accidentally with two wrong numbers; keeping
 * debits and credits separate makes an unbalanced transaction obvious.
 */
public record LedgerEntry(LedgerEntryId id, LedgerAccountRef account, EntryDirection direction, Money amount) {

    public LedgerEntry {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(account, "account");
        Objects.requireNonNull(direction, "direction");
        Objects.requireNonNull(amount, "amount");
        if (!amount.isPositive()) {
            throw new IllegalArgumentException("A ledger entry amount must be strictly positive; direction carries the sign");
        }
    }

    public static LedgerEntry debit(LedgerAccountRef account, Money amount) {
        return new LedgerEntry(LedgerEntryId.generate(), account, EntryDirection.DEBIT, amount);
    }

    public static LedgerEntry credit(LedgerAccountRef account, Money amount) {
        return new LedgerEntry(LedgerEntryId.generate(), account, EntryDirection.CREDIT, amount);
    }

    public boolean isDebit() {
        return direction == EntryDirection.DEBIT;
    }

    public boolean isCredit() {
        return direction == EntryDirection.CREDIT;
    }
}
