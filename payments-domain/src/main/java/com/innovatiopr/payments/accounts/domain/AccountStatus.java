package com.innovatiopr.payments.accounts.domain;

/**
 * Lifecycle of an account.
 *
 * <pre>
 *   ACTIVE  --freeze--&gt;  FROZEN  --unfreeze--&gt;  ACTIVE
 *   ACTIVE  --close---&gt;  CLOSED  (terminal)
 *   FROZEN  --close---&gt;  CLOSED  (terminal)
 * </pre>
 */
public enum AccountStatus {

    /** Can send and receive money. */
    ACTIVE,

    /** Readable, but may neither send nor receive money. */
    FROZEN,

    /** Terminal. No further transactions of any kind. */
    CLOSED;

    public boolean canTransact() {
        return this == ACTIVE;
    }

    public boolean isTerminal() {
        return this == CLOSED;
    }
}
