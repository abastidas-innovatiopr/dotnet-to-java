package com.innovatiopr.payments.accounts.details.application;

import com.innovatiopr.payments.accounts.domain.AccountErrors;
import com.innovatiopr.payments.shared.application.QueryHandler;
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
    public AccountDetails handle(GetAccountQuery query) {
        return accounts.findById(query.accountId())
                .orElseThrow(() -> AccountErrors.notFound(query.accountId()));
    }
}
