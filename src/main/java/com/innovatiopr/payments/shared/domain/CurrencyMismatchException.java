package com.innovatiopr.payments.shared.domain;

import java.util.Currency;

/**
 * Thrown when arithmetic is attempted between two {@link Money} values of different currencies.
 *
 * <h2>Why an exception and not a {@code DomainError}?</h2>
 * Both exist, and they serve different callers:
 * <ul>
 *   <li>{@code MoneyError.CurrencyMismatch} is returned by <em>aggregates</em> when a request supplies the
 *       wrong currency. That is an expected business outcome and becomes an HTTP 422.</li>
 *   <li>This exception is the last-line guard inside {@link Money} itself. Reaching it means a caller
 *       performed arithmetic without first checking {@link Money#sameCurrency(Money)} — a programming
 *       bug, not a business condition.</li>
 * </ul>
 * In other words: the domain never <em>relies</em> on this exception for control flow; it exists so that a
 * bug fails loudly instead of silently producing a wrong balance.
 */
public final class CurrencyMismatchException extends RuntimeException {

    public CurrencyMismatchException(Currency left, Currency right) {
        super("Cannot combine monetary amounts in %s and %s".formatted(left.getCurrencyCode(), right.getCurrencyCode()));
    }
}
