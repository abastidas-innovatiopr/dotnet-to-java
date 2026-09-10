package com.innovatiopr.payments.shared.api;

import com.innovatiopr.payments.shared.application.PageRequest;
import com.innovatiopr.payments.shared.application.SortDirection;
import com.innovatiopr.payments.shared.application.SortSpec;
import org.springframework.web.servlet.function.ServerRequest;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.Optional;

/**
 * Parses pagination, sorting and filter parameters out of a query string.
 *
 * <p>Unparseable values fall back to the default rather than producing a 500. {@code ?page=banana} means
 * "page 0", not an internal error — and {@link PageRequest} then clamps whatever it is given, so no
 * request can ask for an unbounded result set.
 */
public final class PageParams {

    public static final String DEFAULT_SORT_FIELD = "createdAt";

    private PageParams() {
    }

    public static PageRequest from(ServerRequest request) {
        return from(request, DEFAULT_SORT_FIELD);
    }

    public static PageRequest from(ServerRequest request, String defaultSortField) {
        int page = intParam(request, "page", 0);
        int size = intParam(request, "size", PageRequest.DEFAULT_SIZE);
        String sortField = request.param("sort").filter(value -> !value.isBlank()).orElse(defaultSortField);
        SortDirection direction = SortDirection.parse(request.param("direction").orElse(null), SortDirection.DESC);
        return PageRequest.of(page, size, SortSpec.of(sortField, direction));
    }

    public static int intParam(ServerRequest request, String name, int fallback) {
        return request.param(name)
                .map(value -> {
                    try {
                        return Integer.parseInt(value.trim());
                    } catch (NumberFormatException e) {
                        return fallback;
                    }
                })
                .orElse(fallback);
    }

    public static Optional<Instant> instantParam(ServerRequest request, String name) {
        return request.param(name)
                .filter(value -> !value.isBlank())
                .flatMap(value -> {
                    try {
                        return Optional.of(Instant.parse(value.trim()));
                    } catch (DateTimeParseException e) {
                        return Optional.empty();
                    }
                });
    }

    public static Optional<BigDecimal> decimalParam(ServerRequest request, String name) {
        return request.param(name)
                .filter(value -> !value.isBlank())
                .flatMap(value -> {
                    try {
                        return Optional.of(new BigDecimal(value.trim()));
                    } catch (NumberFormatException e) {
                        return Optional.empty();
                    }
                });
    }

    public static Optional<String> stringParam(ServerRequest request, String name) {
        return request.param(name).filter(value -> !value.isBlank()).map(String::trim);
    }
}
