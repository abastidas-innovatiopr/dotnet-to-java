package com.innovatiopr.payments.shared.api;

import com.innovatiopr.payments.shared.application.PageRequest;
import com.innovatiopr.payments.shared.application.SortDirection;
import com.innovatiopr.payments.shared.application.SortSpec;
import io.swagger.v3.oas.annotations.Parameter;
import org.springdoc.core.annotations.ParameterObject;

/**
 * The pagination and sorting parameters every collection endpoint accepts.
 *
 * <h2>Why every field is a {@code String}</h2>
 * Binding {@code page} and {@code size} to {@code Integer} would be the obvious choice and would change
 * the API's behaviour: Spring would reject {@code ?size=banana} with a 400 before the controller ran.
 * This API's documented contract is that an unparseable paging value falls back to the default —
 * {@code ?page=banana} means "page 0", not an internal error, and {@code PageRequest} then clamps
 * whatever it is given so no request can ask for an unbounded result set. Parsing here rather than in the
 * binder keeps that promise.
 *
 * <p>{@code @ParameterObject} tells springdoc to flatten these four components into individual query
 * parameters in the document rather than describing them as a nested object.
 */
@ParameterObject
public record PageQuery(

        @Parameter(description = "Zero-based page number. Negative values normalise to 0.", example = "0")
        String page,

        @Parameter(description = "Page size. Default 20, maximum 100 - larger values are clamped and the "
                + "effective size is echoed back in the response.", example = "20")
        String size,

        @Parameter(description = "Sort field. Unknown values fall back to the collection's default.")
        String sort,

        @Parameter(description = "asc or desc.", example = "desc")
        String direction) {

    public PageRequest toPageRequest(String defaultSortField) {
        String sortField = sort == null || sort.isBlank() ? defaultSortField : sort;
        return PageRequest.of(
                parseInt(page, 0),
                parseInt(size, PageRequest.DEFAULT_SIZE),
                SortSpec.of(sortField, SortDirection.parse(direction, SortDirection.DESC)));
    }

    private static int parseInt(String value, int fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            return fallback;
        }
    }
}
