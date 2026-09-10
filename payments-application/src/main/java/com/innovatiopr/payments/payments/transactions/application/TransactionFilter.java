package com.innovatiopr.payments.payments.transactions.application;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Optional filters for transaction history. Every field is nullable and means "no constraint".
 *
 * <p>Filters are applied in SQL, alongside the same {@code LIMIT}/{@code OFFSET} as the page — so the
 * totals in the pagination metadata describe the filtered set, not the whole table. Getting that wrong is
 * a classic bug: a client sees {@code totalPages: 12} but page 3 is empty because the count ignored the
 * filter.
 */
public record TransactionFilter(String type, String status, Instant dateFrom, Instant dateTo,
                                BigDecimal minimumAmount, BigDecimal maximumAmount) {

    public static TransactionFilter none() {
        return new TransactionFilter(null, null, null, null, null, null);
    }

    public boolean isEmpty() {
        return type == null && status == null && dateFrom == null && dateTo == null
                && minimumAmount == null && maximumAmount == null;
    }
}
