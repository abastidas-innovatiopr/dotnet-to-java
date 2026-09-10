package com.innovatiopr.payments.payments.deposits.application;

import com.innovatiopr.payments.accounts.AccountId;
import com.innovatiopr.payments.shared.application.Command;

import java.math.BigDecimal;

public record DepositMoneyCommand(AccountId accountId, BigDecimal amount, String currencyCode, String reference)
        implements Command { }
