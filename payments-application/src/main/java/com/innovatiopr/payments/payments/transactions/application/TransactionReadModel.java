package com.innovatiopr.payments.payments.transactions.application;

import com.innovatiopr.payments.accounts.AccountId;
import com.innovatiopr.payments.payments.domain.TransactionId;
import com.innovatiopr.payments.shared.application.PageRequest;
import com.innovatiopr.payments.shared.application.PageResult;

import java.util.Optional;

/** Read-side port for transaction history. Implemented with {@code JdbcClient}. */
public interface TransactionReadModel {

    Optional<TransactionDetails> findById(TransactionId id);

    PageResult<TransactionDetails> search(TransactionFilter filter, PageRequest page);

    /** History for one account, matching it as either the source or the destination. */
    PageResult<TransactionDetails> findByAccount(AccountId accountId, TransactionFilter filter, PageRequest page);
}
