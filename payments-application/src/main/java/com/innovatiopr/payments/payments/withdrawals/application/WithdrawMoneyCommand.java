package com.innovatiopr.payments.payments.withdrawals.application;

import com.innovatiopr.payments.accounts.AccountId;
import com.innovatiopr.payments.shared.application.Command;

import java.math.BigDecimal;

public record WithdrawMoneyCommand(AccountId accountId, BigDecimal amount, String currencyCode, String reference)
        implements Command { }
