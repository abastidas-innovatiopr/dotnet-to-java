package com.innovatiopr.payments.payments.transactions.application;

import com.innovatiopr.payments.accounts.AccountId;
import com.innovatiopr.payments.payments.domain.TransactionId;
import com.innovatiopr.payments.shared.application.PageRequest;
import com.innovatiopr.payments.shared.application.Query;

public final class TransactionQueries {

    private TransactionQueries() {
    }

    public record GetTransactionQuery(TransactionId transactionId) implements Query { }

    public record GetTransactionsQuery(TransactionFilter filter, PageRequest page) implements Query { }

    public record GetAccountTransactionsQuery(AccountId accountId, TransactionFilter filter, PageRequest page)
            implements Query { }
}
