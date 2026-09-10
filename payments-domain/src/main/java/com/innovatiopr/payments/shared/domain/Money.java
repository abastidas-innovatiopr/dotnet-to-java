package com.innovatiopr.payments.shared.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Currency;
import java.util.Objects;

/**
 * An immutable monetary amount in a single currency.
 *
 * <h2>Scale, precision and rounding — the contract</h2>
 * <ul>
 *   <li><b>Representation</b>: {@link BigDecimal}. {@code double}/{@code float} are never used for money;
 *       binary floating point cannot represent {@code 0.10} exactly, so sums silently drift.</li>
 *   <li><b>Scale</b>: normalised on construction to the currency's ISO-4217 minor-unit count
 *       ({@link Currency#getDefaultFractionDigits()}) — 2 for USD/EUR, 0 for JPY, 3 for BHD.</li>
 *   <li><b>Rounding</b>: {@link RoundingMode#HALF_EVEN} ("banker's rounding"). Unlike {@code HALF_UP} it
 *       does not bias totals upward across many roundings, which is why it is the norm in finance.</li>
 *   <li><b>Precision</b>: the database column is {@code NUMERIC(19, 4)} — 4 fractional digits so that
 *       currencies with three minor units still fit, and 15 integral digits of headroom.</li>
 * </ul>
 *
 * <h2>Why normalising scale matters for equality</h2>
 * {@code new BigDecimal("10.0").equals(new BigDecimal("10.00"))} is {@code false} — {@code BigDecimal#equals}
 * compares scale as well as value. Because the canonical constructor rescales every amount, two
 * {@code Money} values that represent the same amount are always {@code equals}, and the record's generated
 * {@code equals}/{@code hashCode} behave the way a value object must.
 */
public record Money(BigDecimal amount, Currency currency) implements Comparable<Money> {

    /** Rounding applied whenever an amount must be reduced to its currency's scale. */
    public static final RoundingMode ROUNDING_MODE = RoundingMode.HALF_EVEN;

    public Money {
        Objects.requireNonNull(amount, "amount");
        Objects.requireNonNull(currency, "currency");
        amount = amount.setScale(currency.getDefaultFractionDigits(), ROUNDING_MODE);
    }

    public static Money of(BigDecimal amount, Currency currency) {
        return new Money(amount, currency);
    }

    public static Money of(String amount, Currency currency) {
        return new Money(new BigDecimal(amount), currency);
    }

    /** @throws IllegalArgumentException if {@code currencyCode} is not a valid ISO-4217 code. */
    public static Money of(String amount, String currencyCode) {
        return new Money(new BigDecimal(amount), Currency.getInstance(currencyCode));
    }

    public static Money zero(Currency currency) {
        return new Money(BigDecimal.ZERO, currency);
    }

    public Money add(Money other) {
        requireSameCurrency(other);
        return new Money(amount.add(other.amount), currency);
    }

    public Money subtract(Money other) {
        requireSameCurrency(other);
        return new Money(amount.subtract(other.amount), currency);
    }

    public Money negate() {
        return new Money(amount.negate(), currency);
    }

    public Money abs() {
        return new Money(amount.abs(), currency);
    }

    public boolean sameCurrency(Money other) {
        return currency.equals(other.currency);
    }

    public boolean isPositive() {
        return amount.signum() > 0;
    }

    public boolean isNegative() {
        return amount.signum() < 0;
    }

    public boolean isZero() {
        return amount.signum() == 0;
    }

    public boolean greaterThan(Money other) {
        return compareTo(other) > 0;
    }

    public boolean greaterThanOrEqualTo(Money other) {
        return compareTo(other) >= 0;
    }

    public boolean lessThan(Money other) {
        return compareTo(other) < 0;
    }

    public boolean lessThanOrEqualTo(Money other) {
        return compareTo(other) <= 0;
    }

    @Override
    public int compareTo(Money other) {
        requireSameCurrency(other);
        return amount.compareTo(other.amount);
    }

    @Override
    public String toString() {
        return amount.toPlainString() + " " + currency.getCurrencyCode();
    }

    private void requireSameCurrency(Money other) {
        Objects.requireNonNull(other, "other");
        if (!sameCurrency(other)) {
            throw new CurrencyMismatchException(currency, other.currency);
        }
    }
}
