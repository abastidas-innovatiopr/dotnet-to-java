package com.innovatiopr.payments.ledger.application;

import com.innovatiopr.payments.accounts.AccountId;
import com.innovatiopr.payments.ledger.LedgerApi;
import com.innovatiopr.payments.ledger.LedgerTransactionId;
import com.innovatiopr.payments.ledger.PostingReference;
import com.innovatiopr.payments.ledger.domain.LedgerTransaction;
import com.innovatiopr.payments.shared.application.DomainEventPublisher;
import com.innovatiopr.payments.shared.domain.Money;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.function.Supplier;

/**
 * Implements the Ledger module's published contract.
 *
 * <p>Like the Accounts facade these methods are {@code MANDATORY}: ledger postings only make sense as part
 * of the caller's transaction. A ledger row that committed while the balance change rolled back would be
 * worse than no row at all.
 */
@Service
class LedgerApiAdapter implements LedgerApi {

    private final LedgerRepository ledger;
    private final DomainEventPublisher events;
    private final Clock clock;

    LedgerApiAdapter(LedgerRepository ledger, DomainEventPublisher events, Clock clock) {
        this.ledger = ledger;
        this.events = events;
        this.clock = clock;
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public LedgerTransactionId recordTransfer(PostingReference reference, AccountId source,
                                              AccountId destination, Money amount, String description) {
        return record(() -> LedgerTransaction.recordTransfer(reference, source, destination, amount,
                description, clock.instant()));
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public LedgerTransactionId recordDeposit(PostingReference reference, AccountId account,
                                             Money amount, String description) {
        return record(() -> LedgerTransaction.recordDeposit(reference, account, amount, description,
                clock.instant()));
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public LedgerTransactionId recordWithdrawal(PostingReference reference, AccountId account,
                                                Money amount, String description) {
        return record(() -> LedgerTransaction.recordWithdrawal(reference, account, amount, description,
                clock.instant()));
    }

    private LedgerTransactionId record(Supplier<LedgerTransaction> factory) {
        LedgerTransaction transaction = factory.get();
        ledger.save(transaction);
        events.publishFrom(transaction);
        return transaction.id();
    }
}
