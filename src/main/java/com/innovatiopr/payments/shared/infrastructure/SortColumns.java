package com.innovatiopr.payments.shared.infrastructure;

import com.innovatiopr.payments.shared.application.SortSpec;

import java.util.Map;
import java.util.Objects;

/**
 * Translates a client-supplied sort field into a SQL fragment, against an explicit allow-list.
 *
 * <h2>Why an allow-list and not escaping</h2>
 * A sort field cannot be a bind parameter — {@code ORDER BY ?} is not valid SQL — so the column name has
 * to be part of the statement text. That means the only safe approach is to never let client input reach
 * the string at all: the request names a <em>logical</em> field, this class looks it up, and an unknown
 * name falls back to the default rather than being passed through. There is no code path in which a
 * query-string value becomes SQL.
 *
 * <h2>Deterministic ordering</h2>
 * Every mapping appends a unique tiebreaker ({@code id}). Sorting by {@code created_at} alone is not a
 * total order — rows written in the same transaction share a timestamp — and with {@code LIMIT}/
 * {@code OFFSET} an unstable order means a row can appear on two consecutive pages or on neither. The
 * tiebreaker is what makes paging through a list actually enumerate it.
 */
public final class SortColumns {

    private final Map<String, String> allowed;
    private final String defaultColumn;
    private final String tiebreakerColumn;

    /**
     * @param allowed          logical field name to SQL column name
     * @param defaultColumn    SQL column used when the request names nothing valid
     * @param tiebreakerColumn unique column appended to every ordering
     */
    public SortColumns(Map<String, String> allowed, String defaultColumn, String tiebreakerColumn) {
        this.allowed = Map.copyOf(allowed);
        this.defaultColumn = Objects.requireNonNull(defaultColumn, "defaultColumn");
        this.tiebreakerColumn = Objects.requireNonNull(tiebreakerColumn, "tiebreakerColumn");
    }

    /** @return a SQL fragment such as {@code created_at DESC, id DESC} — never containing client text. */
    public String orderByClause(SortSpec sort) {
        String column = allowed.getOrDefault(sort.field(), defaultColumn);
        String direction = sort.direction().sql();
        if (column.equals(tiebreakerColumn)) {
            return column + " " + direction;
        }
        return column + " " + direction + ", " + tiebreakerColumn + " " + direction;
    }

    public boolean supports(String field) {
        return allowed.containsKey(field);
    }

    public java.util.Set<String> supportedFields() {
        return allowed.keySet();
    }
}
