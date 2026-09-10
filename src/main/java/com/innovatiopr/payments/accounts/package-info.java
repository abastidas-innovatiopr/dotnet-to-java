/**
 * Accounts bounded context: balances, account lifecycle and the invariants that protect them.
 *
 * <p>Depends on Customers only to confirm that a customer exists before opening an account for them, and
 * does so through {@code CustomersApi} rather than by reading the {@code customers} table.
 *
 * <p>{@code allowedDependencies} makes the module graph an explicit, verified declaration rather than
 * whatever the imports happen to add up to. Adding an import of {@code payments} here would fail
 * {@code ModularityTest}, not merely be noticed in review.
 */
@org.springframework.modulith.ApplicationModule(
        displayName = "Accounts",
        allowedDependencies = {"customers"})
package com.innovatiopr.payments.accounts;
