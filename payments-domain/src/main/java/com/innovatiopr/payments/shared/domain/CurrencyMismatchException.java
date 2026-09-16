package com.innovatiopr.payments.shared.domain;

import java.util.Currency;

/**
 * Thrown when arithmetic is attempted between two {@link Money} values of different currencies.
 *
 * <h2>Why this is not a {@code DomainException}</h2>
 * Everything in this application's failure model is now an exception, so the distinction is no longer
 * "exception versus return value" — it is <em>expected</em> versus <em>impossible</em>:
 * <ul>
 *   <li>{@code MoneyErrors.currencyMismatch(...)} is thrown by <em>aggregates</em> when a request supplies
 *       the wrong currency. That is an expected business outcome and becomes an HTTP 422.</li>
 *   <li>This exception is the last-line guard inside {@link Money} itself. Reaching it means a caller
 *       performed arithmetic without first checking {@link Money#sameCurrency(Money)} — a programming
 *       bug, not a business condition, and the API layer renders it as a 500 rather than a 422.</li>
 * </ul>
 * In other words: the domain never <em>relies</em> on this exception for control flow; it exists so that a
 * bug fails loudly instead of silently producing a wrong balance.
 */
public final class CurrencyMismatchException extends RuntimeException {

    public CurrencyMismatchException(Currency left, Currency right) {
        super("Cannot combine monetary amounts in %s and %s".formatted(left.getCurrencyCode(), right.getCurrencyCode()));
    }
}
