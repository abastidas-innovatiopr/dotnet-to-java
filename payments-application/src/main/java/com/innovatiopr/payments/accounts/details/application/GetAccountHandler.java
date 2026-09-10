package com.innovatiopr.payments.accounts.details.application;

import com.innovatiopr.payments.accounts.domain.AccountError;
import com.innovatiopr.payments.shared.application.QueryHandler;
import com.innovatiopr.payments.shared.domain.Result;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GetAccountHandler implements QueryHandler<GetAccountQuery, AccountDetails> {

    private final AccountReadModel accounts;

    public GetAccountHandler(AccountReadModel accounts) {
        this.accounts = accounts;
    }

    @Override
    @Transactional(readOnly = true)
    public Result<AccountDetails> handle(GetAccountQuery query) {
        return accounts.findById(query.accountId())
                .map(Result::success)
                .orElseGet(() -> Result.failure(AccountError.notFound(query.accountId())));
    }
}
