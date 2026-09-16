package com.innovatiopr.payments.ledger.domain;

import com.innovatiopr.payments.accounts.AccountId;
import com.innovatiopr.payments.ledger.LedgerTransactionId;
import com.innovatiopr.payments.ledger.PostingReference;
import com.innovatiopr.payments.shared.domain.AggregateRoot;
import com.innovatiopr.payments.shared.domain.Money;

import java.time.Instant;
import java.util.Currency;
import java.util.List;
import java.util.Objects;

/**
 * A balanced set of double-entry postings — the aggregate root of the Ledger context.
 *
 * <h2>The invariant this type exists to protect</h2>
 * Total debits must equal total credits, in a single currency. That check happens in one place, in
 * {@link #create}, and there is no way to construct a {@code LedgerTransaction} that skips it: the
 * constructor is private and every factory funnels through the same validation.
 *
 * <p>This is why application code never assembles debit and credit rows itself. If a handler could write
 * arbitrary entries, "the ledger balances" would be a convention that every new use case has to remember,
 * rather than a property of the type. Callers ask for {@link #recordTransfer}, {@link #recordDeposit} or
 * {@link #recordWithdrawal}, and the aggregate produces the correct pair.
 *
 * <p>The invariant is also enforced in PostgreSQL: {@code ledger_transactions} stores {@code total_debits}
 * and {@code total_credits} with a {@code CHECK (total_debits = total_credits)}.
 */
public final class LedgerTransaction extends AggregateRoot<LedgerTransactionId> {

    private final LedgerTransactionId id;
    private final PostingReference reference;
    private final List<LedgerEntry> entries;
    private final String description;
    private final Instant recordedAt;

    private LedgerTransaction(LedgerTransactionId id, PostingReference reference, List<LedgerEntry> entries,
                              String description, Instant recordedAt) {
        this.id = id;
        this.reference = reference;
        this.entries = List.copyOf(entries);
        this.description = description;
        this.recordedAt = recordedAt;
    }

    /** Two internal accounts: money leaves one and arrives at the other. */
    public static LedgerTransaction recordTransfer(PostingReference reference, AccountId source,
                                                   AccountId destination, Money amount,
                                                   String description, Instant now) {
        return create(reference, List.of(
                LedgerEntry.debit(LedgerAccountRef.internal(source), amount),
                LedgerEntry.credit(LedgerAccountRef.internal(destination), amount)), description, now);
    }

    /** Cash in: the customer account is credited, the settlement position is debited. */
    public static LedgerTransaction recordDeposit(PostingReference reference, AccountId account,
                                                  Money amount, String description, Instant now) {
        return create(reference, List.of(
                LedgerEntry.debit(LedgerAccountRef.externalSettlement(), amount),
                LedgerEntry.credit(LedgerAccountRef.internal(account), amount)), description, now);
    }

    /** Cash out: the customer account is debited, the settlement position is credited. */
    public static LedgerTransaction recordWithdrawal(PostingReference reference, AccountId account,
                                                     Money amount, String description, Instant now) {
        return create(reference, List.of(
                LedgerEntry.debit(LedgerAccountRef.internal(account), amount),
                LedgerEntry.credit(LedgerAccountRef.externalSettlement(), amount)), description, now);
    }

    private static LedgerTransaction create(PostingReference reference, List<LedgerEntry> entries,
                                            String description, Instant now) {
        Objects.requireNonNull(reference, "reference");
        Objects.requireNonNull(entries, "entries");

        if (entries.size() < 2) {
            throw LedgerErrors.tooFewEntries(entries.size());
        }

        Currency currency = entries.getFirst().amount().currency();
        boolean mixed = entries.stream().anyMatch(entry -> !entry.amount().currency().equals(currency));
        if (mixed) {
            throw LedgerErrors.mixedCurrencies();
        }

        Money debits = sum(entries, EntryDirection.DEBIT, currency);
        Money credits = sum(entries, EntryDirection.CREDIT, currency);
        if (!debits.equals(credits)) {
            throw LedgerErrors.unbalanced(debits, credits);
        }

        LedgerTransaction transaction = new LedgerTransaction(
                LedgerTransactionId.generate(), reference, entries, description, now);
        transaction.raise(LedgerTransactionRecorded.of(transaction.id, reference, debits.amount(),
                currency.getCurrencyCode(), entries.size(), now));
        return transaction;
    }

    /** Rebuilds from storage: no validation, no events. */
    public static LedgerTransaction reconstitute(LedgerTransactionId id, PostingReference reference,
                                                 List<LedgerEntry> entries, String description, Instant recordedAt) {
        return new LedgerTransaction(id, reference, entries, description, recordedAt);
    }

    private static Money sum(List<LedgerEntry> entries, EntryDirection direction, Currency currency) {
        return entries.stream()
                .filter(entry -> entry.direction() == direction)
                .map(LedgerEntry::amount)
                .reduce(Money.zero(currency), Money::add);
    }

    public Money totalDebits() {
        return sum(entries, EntryDirection.DEBIT, currency());
    }

    public Money totalCredits() {
        return sum(entries, EntryDirection.CREDIT, currency());
    }

    /** True when the aggregate's defining invariant holds. Used by tests and by the persistence guard. */
    public boolean isBalanced() {
        return totalDebits().equals(totalCredits());
    }

    public Currency currency() {
        return entries.getFirst().amount().currency();
    }

    @Override
    public LedgerTransactionId id() {
        return id;
    }

    public PostingReference reference() {
        return reference;
    }

    public List<LedgerEntry> entries() {
        return entries;
    }

    public String description() {
        return description;
    }

    public Instant recordedAt() {
        return recordedAt;
    }
}
