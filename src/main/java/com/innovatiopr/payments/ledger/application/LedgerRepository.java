package com.innovatiopr.payments.ledger.application;

import com.innovatiopr.payments.ledger.LedgerTransactionId;
import com.innovatiopr.payments.ledger.PostingReference;
import com.innovatiopr.payments.ledger.domain.LedgerTransaction;

import java.util.Optional;

/** The Ledger module's persistence port. The ledger is append-only: there is no update and no delete. */
public interface LedgerRepository {

    Optional<LedgerTransaction> findById(LedgerTransactionId id);

    Optional<LedgerTransaction> findByReference(PostingReference reference);

    void save(LedgerTransaction transaction);
}
