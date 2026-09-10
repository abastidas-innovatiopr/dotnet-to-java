package com.innovatiopr.payments.shared.application;

import java.util.List;
import java.util.Objects;

/**
 * A framework-neutral page of results, returned by query handlers and translated into a Spring HATEOAS
 * {@code PagedModel} at the API boundary.
 *
 * @param items      the rows on this page
 * @param page       zero-based page number actually served
 * @param size       effective page size after normalisation
 * @param totalItems total rows matching the filter, across all pages
 */
public record PageResult<T>(List<T> items, int page, int size, long totalItems) {

    public PageResult {
        Objects.requireNonNull(items, "items");
        items = List.copyOf(items);
    }

    public static <T> PageResult<T> of(List<T> items, PageRequest request, long totalItems) {
        return new PageResult<>(items, request.page(), request.size(), totalItems);
    }

    public static <T> PageResult<T> empty(PageRequest request) {
        return new PageResult<>(List.of(), request.page(), request.size(), 0);
    }

    public int totalPages() {
        if (size <= 0) {
            return 0;
        }
        return (int) ((totalItems + size - 1) / size);
    }

    public boolean hasNext() {
        return page + 1 < totalPages();
    }

    public boolean hasPrevious() {
        return page > 0 && totalPages() > 0;
    }

    public boolean isEmpty() {
        return items.isEmpty();
    }

    public <R> PageResult<R> map(java.util.function.Function<? super T, R> mapper) {
        return new PageResult<>(items.stream().map(mapper).toList(), page, size, totalItems);
    }
}
