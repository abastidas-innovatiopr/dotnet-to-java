package com.innovatiopr.payments.shared.domain;

import java.util.Currency;

/** Expected business failures relating to monetary amounts. */
public final class MoneyErrors {

    private MoneyErrors() {
    }

    public static DomainException currencyMismatch(Currency expected, Currency actual) {
        return new DomainException(
                "MONEY_CURRENCY_MISMATCH",
                "Expected an amount in %s but received %s"
                        .formatted(expected.getCurrencyCode(), actual.getCurrencyCode()));
    }

    public static ValidationException amountMustBePositive() {
        return new ValidationException("MONEY_INVALID_AMOUNT", "Amount must be greater than zero");
    }

    public static ValidationException unknownCurrency(String currencyCode) {
        return new ValidationException(
                "MONEY_UNKNOWN_CURRENCY",
                "'%s' is not a recognised ISO-4217 currency code".formatted(currencyCode));
    }
}
