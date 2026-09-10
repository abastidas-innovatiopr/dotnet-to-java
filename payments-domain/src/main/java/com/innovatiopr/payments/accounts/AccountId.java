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
     *
     * <p><b>Note that this order is not the one you would guess.</b> {@link UUID#compareTo} compares the
     * two 64-bit halves as <em>signed</em> longs, so {@code ffffffff-...} (whose high half is -1) sorts
     * <em>before</em> {@code 00000000-...-0001}. The ordering is therefore not the lexicographic or
     * unsigned-numeric order the string form suggests.
     *
     * <p>That does not matter here, and it is worth being clear why: deadlock avoidance requires only that
     * every transaction agrees on <em>some</em> consistent total order. Which order is irrelevant, as long
     * as it is the same one everywhere. It would matter if this comparator were also used to sort ids for
     * display or for keyset pagination — so it is not.
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
