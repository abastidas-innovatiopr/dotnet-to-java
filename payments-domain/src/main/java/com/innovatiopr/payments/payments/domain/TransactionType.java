package com.innovatiopr.payments.payments.domain;

public enum TransactionType {

    /** Money moved between two internal accounts. */
    TRANSFER,

    /** Cash in from outside the system. */
    DEPOSIT,

    /** Cash out of the system. */
    WITHDRAWAL
}
