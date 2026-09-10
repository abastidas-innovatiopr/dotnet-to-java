package com.innovatiopr.payments.accounts;

import com.innovatiopr.payments.shared.domain.Money;

/** The effect of one balance movement, as reported back to the calling module. */
public record AccountPosting(AccountId accountId, Money balanceAfter) { }
