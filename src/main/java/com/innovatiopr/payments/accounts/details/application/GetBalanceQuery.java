package com.innovatiopr.payments.accounts.details.application;

import com.innovatiopr.payments.accounts.AccountId;
import com.innovatiopr.payments.shared.application.Query;

public record GetBalanceQuery(AccountId accountId) implements Query { }
