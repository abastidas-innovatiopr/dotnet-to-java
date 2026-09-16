package com.innovatiopr.payments.payments.transactions.application;

import com.innovatiopr.payments.payments.domain.TransactionErrors;
import com.innovatiopr.payments.shared.application.QueryHandler;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GetTransactionHandler
        implements QueryHandler<TransactionQueries.GetTransactionQuery, TransactionDetails> {

    private final TransactionReadModel transactions;

    public GetTransactionHandler(TransactionReadModel transactions) {
        this.transactions = transactions;
    }

    @Override
    @Transactional(readOnly = true)
    public TransactionDetails handle(TransactionQueries.GetTransactionQuery query) {
        return transactions.findById(query.transactionId())
                .orElseThrow(() -> TransactionErrors.notFound(query.transactionId().toString()));
    }
}
