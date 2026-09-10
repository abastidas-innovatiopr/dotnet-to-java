/**
 * Payments bounded context: transfers, deposits, withdrawals, transaction history and idempotency.
 *
 * <p>The orchestrating module. It coordinates Accounts (balance movements) and Ledger (postings) inside a
 * single transaction, and owns everything about a financial operation that neither of those modules
 * should know: the payment transaction's lifecycle and the idempotency guarantee.
 *
 * <p>Nothing depends on Payments, which is what makes it the right place for orchestration.
 */
@org.springframework.modulith.ApplicationModule(
        displayName = "Payments",
        allowedDependencies = {"accounts", "ledger"})
package com.innovatiopr.payments.payments;
