package com.innovatiopr.payments.ledger.statements.application;

import com.innovatiopr.payments.accounts.AccountId;
import com.innovatiopr.payments.shared.application.PageRequest;
import com.innovatiopr.payments.shared.application.PageResult;

import java.time.Instant;

/** Read-side port for account statements. Implemented with {@code JdbcClient}. */
public interface StatementReadModel {

    PageResult<StatementLine> findForAccount(AccountId accountId, Instant from, Instant to, PageRequest page);
}
