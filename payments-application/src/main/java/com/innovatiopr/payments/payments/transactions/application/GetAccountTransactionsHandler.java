package com.innovatiopr.payments.payments.transactions.application;

import com.innovatiopr.payments.accounts.AccountsApi;
import com.innovatiopr.payments.shared.application.PageResult;
import com.innovatiopr.payments.shared.application.QueryHandler;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * History for one account.
 *
 * <p>Checks that the account exists before querying, so a bad id returns 404 rather than an empty page.
 * "No rows" and "no such account" are different answers and clients need to tell them apart.
 */
@Service
public class GetAccountTransactionsHandler
        implements QueryHandler<TransactionQueries.GetAccountTransactionsQuery, PageResult<TransactionDetails>> {

    private final TransactionReadModel transactions;
    private final AccountsApi accounts;

    public GetAccountTransactionsHandler(TransactionReadModel transactions, AccountsApi accounts) {
        this.transactions = transactions;
        this.accounts = accounts;
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<TransactionDetails> handle(TransactionQueries.GetAccountTransactionsQuery query) {
        accounts.requireExists(query.accountId());
        return transactions.findByAccount(query.accountId(), query.filter(), query.page());
    }
}
