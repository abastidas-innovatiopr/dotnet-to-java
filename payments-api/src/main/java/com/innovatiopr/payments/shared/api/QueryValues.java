package com.innovatiopr.payments.shared.api;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.Optional;

/**
 * Lenient parsing for optional filter parameters.
 *
 * <p>Same principle as {@link PageQuery}: a filter value that cannot be parsed is treated as absent
 * rather than as a 400. A client that sends {@code ?dateFrom=yesterday} gets the unfiltered collection,
 * not an error — the filter is an optional narrowing, and refusing the whole request over one
 * uninterpretable narrowing is a worse answer than ignoring it.
 */
public final class QueryValues {

    private QueryValues() {
    }

    public static Optional<Instant> instant(String value) {
        return text(value).flatMap(trimmed -> {
            try {
                return Optional.of(Instant.parse(trimmed));
            } catch (DateTimeParseException e) {
                return Optional.empty();
            }
        });
    }

    public static Optional<BigDecimal> decimal(String value) {
        return text(value).flatMap(trimmed -> {
            try {
                return Optional.of(new BigDecimal(trimmed));
            } catch (NumberFormatException e) {
                return Optional.empty();
            }
        });
    }

    public static Optional<String> text(String value) {
        return Optional.ofNullable(value).filter(candidate -> !candidate.isBlank()).map(String::trim);
    }
}
