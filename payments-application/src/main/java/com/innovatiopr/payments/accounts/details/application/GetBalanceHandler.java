package com.innovatiopr.payments.accounts.details.application;

import com.innovatiopr.payments.accounts.domain.AccountError;
import com.innovatiopr.payments.shared.application.QueryHandler;
import com.innovatiopr.payments.shared.domain.Result;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GetBalanceHandler implements QueryHandler<GetBalanceQuery, AccountBalance> {

    private final AccountReadModel accounts;

    public GetBalanceHandler(AccountReadModel accounts) {
        this.accounts = accounts;
    }

    @Override
    @Transactional(readOnly = true)
    public Result<AccountBalance> handle(GetBalanceQuery query) {
        return accounts.findBalance(query.accountId())
                .map(Result::success)
                .orElseGet(() -> Result.failure(AccountError.notFound(query.accountId())));
    }
}
