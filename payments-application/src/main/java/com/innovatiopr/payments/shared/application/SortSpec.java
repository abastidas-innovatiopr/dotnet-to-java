package com.innovatiopr.payments.shared.application;

import java.util.Objects;

/**
 * A requested sort: a <em>logical</em> field name plus a direction.
 *
 * <p>The field name here is the name a client uses ({@code createdAt}, {@code amount}), never a SQL column.
 * Translation to a column happens in the infrastructure layer against an explicit allow-list — see
 * {@code shared.infrastructure.SortColumns}. Nothing from the query string is ever concatenated into SQL.
 */
public record SortSpec(String field, SortDirection direction) {

    public SortSpec {
        Objects.requireNonNull(field, "field");
        Objects.requireNonNull(direction, "direction");
    }

    public static SortSpec of(String field, SortDirection direction) {
        return new SortSpec(field, direction);
    }

    public static SortSpec descending(String field) {
        return new SortSpec(field, SortDirection.DESC);
    }
}
