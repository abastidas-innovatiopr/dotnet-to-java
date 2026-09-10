package com.innovatiopr.payments.accounts;

/** The effect of both legs of a transfer. */
public record TransferPostings(AccountPosting source, AccountPosting destination) { }
