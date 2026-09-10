package com.innovatiopr.payments.accounts.opening.application;

import com.innovatiopr.payments.customers.CustomerId;
import com.innovatiopr.payments.shared.application.Command;

import java.math.BigDecimal;

public record OpenAccountCommand(CustomerId customerId, String currencyCode, BigDecimal initialDeposit)
        implements Command { }
