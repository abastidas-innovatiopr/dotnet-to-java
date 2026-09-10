package com.innovatiopr.payments.payments.domain;

/**
 * Lifecycle of a payment transaction.
 *
 * <pre>
 *   PENDING --complete--&gt; COMPLETED  (terminal)
 *   PENDING --fail-----&gt; FAILED     (terminal)
 * </pre>
 *
 * Both end states are terminal: a completed transaction can never return to pending. That rule is enforced
 * by {@code PaymentTransaction} and is exactly the sort of invariant a status column plus scattered
 * {@code if} statements fails to guarantee.
 */
public enum TransactionStatus {

    PENDING, COMPLETED, FAILED;

    public boolean isTerminal() {
        return this != PENDING;
    }

    public boolean canTransitionTo(TransactionStatus target) {
        return this == PENDING && target.isTerminal();
    }
}
