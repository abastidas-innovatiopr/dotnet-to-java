package com.innovatiopr.payments.payments.transactions.application;

import com.innovatiopr.payments.shared.application.PageResult;
import com.innovatiopr.payments.shared.application.QueryHandler;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GetTransactionsHandler
        implements QueryHandler<TransactionQueries.GetTransactionsQuery, PageResult<TransactionDetails>> {

    private final TransactionReadModel transactions;

    public GetTransactionsHandler(TransactionReadModel transactions) {
        this.transactions = transactions;
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<TransactionDetails> handle(TransactionQueries.GetTransactionsQuery query) {
        return transactions.search(query.filter(), query.page());
    }
}
