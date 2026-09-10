package com.innovatiopr.payments.ledger.domain;

/**
 * The side of a double-entry posting.
 *
 * <p>In the bank's books, a customer's deposit account is a <em>liability</em>: the bank owes the customer
 * that money. So money leaving a customer account is a DEBIT and money arriving is a CREDIT, which matches
 * the everyday reading of a bank statement.
 */
public enum EntryDirection {

    /** Money out of the referenced account. */
    DEBIT,

    /** Money into the referenced account. */
    CREDIT;

    public EntryDirection opposite() {
        return this == DEBIT ? CREDIT : DEBIT;
    }
}
