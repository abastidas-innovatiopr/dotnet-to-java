package com.innovatiopr.payments.shared.application;

import java.util.Objects;

/**
 * A framework-neutral page request.
 *
 * <p>Spring Data's {@code Pageable} deliberately does not appear here, or anywhere in the domain or
 * application layers. It is a persistence abstraction; letting it leak would couple every use case and
 * every API response to Spring Data, and an ArchUnit rule fails the build if it ever does.
 *
 * <h2>Normalisation</h2>
 * Out-of-range input is normalised rather than rejected, and the effective values are echoed back in the
 * response metadata so a client can always see what it actually got:
 * <ul>
 *   <li>{@code page < 0} becomes {@code 0}</li>
 *   <li>{@code size < 1} becomes {@link #DEFAULT_SIZE}</li>
 *   <li>{@code size > }{@link #MAX_SIZE} is clamped to {@link #MAX_SIZE}, so no client can ask for an
 *       unbounded result set</li>
 * </ul>
 */
public record PageRequest(int page, int size, SortSpec sort) {

    public static final int DEFAULT_SIZE = 20;
    public static final int MAX_SIZE = 100;

    public PageRequest {
        Objects.requireNonNull(sort, "sort");
        page = Math.max(page, 0);
        size = size < 1 ? DEFAULT_SIZE : Math.min(size, MAX_SIZE);
    }

    public static PageRequest of(int page, int size, SortSpec sort) {
        return new PageRequest(page, size, sort);
    }

    /** Row offset for the underlying SQL. Widened to {@code long} so deep paging cannot overflow. */
    public long offset() {
        return (long) page * size;
    }

    public int limit() {
        return size;
    }
}
