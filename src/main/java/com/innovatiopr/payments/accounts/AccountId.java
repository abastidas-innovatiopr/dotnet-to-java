package com.innovatiopr.payments.accounts;

import java.util.Objects;
import java.util.UUID;

/** Identity of an {@code Account} aggregate. Published so Payments and Ledger can reference accounts. */
public record AccountId(UUID value) implements Comparable<AccountId> {

    public AccountId {
        Objects.requireNonNull(value, "value");
    }

    public static AccountId generate() {
        return new AccountId(UUID.randomUUID());
    }

    public static AccountId of(UUID value) {
        return new AccountId(value);
    }

    public static AccountId parse(String raw) {
        return new AccountId(UUID.fromString(raw));
    }

    /**
     * Total order over account identities.
     *
     * <p>This exists for deadlock avoidance, not for display. When a transfer locks two accounts it always
     * locks them in ascending id order, so two concurrent transfers A→B and B→A request the same two row
     * locks in the same sequence and one simply waits instead of deadlocking with the other.
     */
    @Override
    public int compareTo(AccountId other) {
        return value.compareTo(other.value);
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
