package com.innovatiopr.payments.shared.domain;

import java.util.Currency;

/**
 * Expected business failures relating to monetary amounts.
 *
 * <p>Sealed with nested records, so a {@code switch} over {@code MoneyError} is exhaustive without a
 * {@code default} branch.
 */
public sealed interface MoneyError extends DomainError {

    record CurrencyMismatch(Currency expected, Currency actual) implements MoneyError {
        @Override
        public String code() {
            return "MONEY_CURRENCY_MISMATCH";
        }

        @Override
        public String message() {
            return "Expected an amount in %s but received %s"
                    .formatted(expected.getCurrencyCode(), actual.getCurrencyCode());
        }

        @Override
        public ErrorType type() {
            return ErrorType.BUSINESS_RULE;
        }
    }

    record InvalidAmount(String reason) implements MoneyError {
        @Override
        public String code() {
            return "MONEY_INVALID_AMOUNT";
        }

        @Override
        public String message() {
            return reason;
        }

        @Override
        public ErrorType type() {
            return ErrorType.VALIDATION;
        }
    }

    record UnknownCurrency(String currencyCode) implements MoneyError {
        @Override
        public String code() {
            return "MONEY_UNKNOWN_CURRENCY";
        }

        @Override
        public String message() {
            return "'%s' is not a recognised ISO-4217 currency code".formatted(currencyCode);
        }

        @Override
        public ErrorType type() {
            return ErrorType.VALIDATION;
        }
    }

    static MoneyError currencyMismatch(Currency expected, Currency actual) {
        return new CurrencyMismatch(expected, actual);
    }

    static MoneyError amountMustBePositive() {
        return new InvalidAmount("Amount must be greater than zero");
    }
}
