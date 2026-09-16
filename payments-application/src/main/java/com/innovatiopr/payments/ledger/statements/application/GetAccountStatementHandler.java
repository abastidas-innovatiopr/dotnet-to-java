package com.innovatiopr.payments.ledger.statements.application;

import com.innovatiopr.payments.accounts.AccountsApi;
import com.innovatiopr.payments.shared.application.PageResult;
import com.innovatiopr.payments.shared.application.QueryHandler;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GetAccountStatementHandler
        implements QueryHandler<GetAccountStatementQuery, PageResult<StatementLine>> {

    private final StatementReadModel statements;
    private final AccountsApi accounts;

    public GetAccountStatementHandler(StatementReadModel statements, AccountsApi accounts) {
        this.statements = statements;
        this.accounts = accounts;
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<StatementLine> handle(GetAccountStatementQuery query) {
        accounts.requireExists(query.accountId());
        return statements.findForAccount(query.accountId(), query.from(), query.to(), query.page());
    }
}
