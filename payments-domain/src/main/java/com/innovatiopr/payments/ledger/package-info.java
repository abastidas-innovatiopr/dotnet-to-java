/**
 * Ledger bounded context: the append-only, double-entry record of every money movement.
 *
 * <p>Depends on Accounts for {@code AccountId} alone. It deliberately does <em>not</em> depend on
 * Payments: it refers to the operation that caused a posting through its own {@code PostingReference},
 * which is what keeps {@code payments → ledger} a one-way arrow instead of a cycle.
 */
@org.springframework.modulith.ApplicationModule(
        displayName = "Ledger",
        allowedDependencies = {"accounts"})
package com.innovatiopr.payments.ledger;
