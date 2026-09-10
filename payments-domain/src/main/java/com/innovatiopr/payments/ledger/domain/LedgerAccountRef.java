package com.innovatiopr.payments.ledger.domain;

import com.innovatiopr.payments.accounts.AccountId;

import java.util.Objects;
import java.util.Optional;

/**
 * What a ledger entry is posted against.
 *
 * <h2>Why an external leg is needed</h2>
 * Double entry only balances if every posting has a counterparty. A transfer is easy — both legs are
 * customer accounts. A <em>deposit</em> is not: money arrives from outside the system, so the credit to the
 * customer account must be balanced by a debit against something. Without that leg the invariant
 * "total debits equal total credits" would simply be false for every deposit and withdrawal, and the
 * ledger would not be a ledger.
 *
 * <p>{@code External} represents that boundary — a settlement/clearing position, the same role a
 * {@code cash} or {@code nostro} account plays in a real institution.
 *
 * <p>Modelled as a sealed interface so a {@code switch} over the two cases is exhaustive and adding a third
 * kind of counterparty later becomes a compile error at every site that must handle it.
 */
public sealed interface LedgerAccountRef {

    /** A customer account held in the Accounts module. */
    record Internal(AccountId accountId) implements LedgerAccountRef {
        public Internal {
            Objects.requireNonNull(accountId, "accountId");
        }
    }

    /** A settlement position outside the Accounts module. */
    record External(String name) implements LedgerAccountRef {
        public External {
            Objects.requireNonNull(name, "name");
            if (name.isBlank()) {
                throw new IllegalArgumentException("External ledger account name must not be blank");
            }
        }
    }

    /** The single settlement position used for cash in and out of the system. */
    String EXTERNAL_SETTLEMENT = "EXTERNAL_SETTLEMENT";

    static LedgerAccountRef internal(AccountId accountId) {
        return new Internal(accountId);
    }

    static LedgerAccountRef externalSettlement() {
        return new External(EXTERNAL_SETTLEMENT);
    }

    default Optional<AccountId> internalAccountId() {
        return this instanceof Internal(AccountId accountId) ? Optional.of(accountId) : Optional.empty();
    }

    default Optional<String> externalName() {
        return this instanceof External(String name) ? Optional.of(name) : Optional.empty();
    }
}
