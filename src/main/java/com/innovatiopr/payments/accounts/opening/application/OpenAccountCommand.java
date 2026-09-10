package com.innovatiopr.payments.accounts.opening.application;

import com.innovatiopr.payments.customers.CustomerId;
import com.innovatiopr.payments.shared.application.Command;

/**
 * Open a new account.
 *
 * <p>There is deliberately no opening balance: a new account starts empty and is funded by a deposit,
 * which is a Payments operation that writes ledger postings. See {@code Account.open} for why.
 */
public record OpenAccountCommand(CustomerId customerId, String currencyCode) implements Command { }
