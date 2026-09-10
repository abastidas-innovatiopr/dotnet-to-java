package com.innovatiopr.payments.ledger.statements.application;

import com.innovatiopr.payments.accounts.AccountsApi;
import com.innovatiopr.payments.accounts.domain.AccountError;
import com.innovatiopr.payments.shared.application.PageResult;
import com.innovatiopr.payments.shared.application.QueryHandler;
import com.innovatiopr.payments.shared.domain.Result;
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
    public Result<PageResult<StatementLine>> handle(GetAccountStatementQuery query) {
        if (!accounts.exists(query.accountId())) {
            return Result.failure(AccountError.notFound(query.accountId()));
        }
        return Result.success(statements.findForAccount(query.accountId(), query.from(), query.to(), query.page()));
    }
}
