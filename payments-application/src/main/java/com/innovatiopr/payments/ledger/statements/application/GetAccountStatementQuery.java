package com.innovatiopr.payments.ledger.statements.application;

import com.innovatiopr.payments.accounts.AccountId;
import com.innovatiopr.payments.shared.application.PageRequest;
import com.innovatiopr.payments.shared.application.Query;

import java.time.Instant;

public record GetAccountStatementQuery(AccountId accountId, Instant from, Instant to, PageRequest page)
        implements Query { }
