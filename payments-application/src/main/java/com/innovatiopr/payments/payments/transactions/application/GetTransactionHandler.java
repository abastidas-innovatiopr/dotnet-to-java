package com.innovatiopr.payments.payments.transactions.application;

import com.innovatiopr.payments.payments.domain.TransactionError;
import com.innovatiopr.payments.shared.application.QueryHandler;
import com.innovatiopr.payments.shared.domain.Result;
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
    public Result<TransactionDetails> handle(TransactionQueries.GetTransactionQuery query) {
        return transactions.findById(query.transactionId())
                .map(Result::success)
                .orElseGet(() -> Result.failure(TransactionError.notFound(query.transactionId().toString())));
    }
}
