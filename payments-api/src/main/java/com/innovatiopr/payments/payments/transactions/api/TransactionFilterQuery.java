package com.innovatiopr.payments.payments.transactions.api;

import com.innovatiopr.payments.payments.transactions.application.TransactionFilter;
import com.innovatiopr.payments.shared.api.QueryValues;
import io.swagger.v3.oas.annotations.Parameter;
import org.springdoc.core.annotations.ParameterObject;

/**
 * The transaction-history filters, shared by both collection endpoints.
 *
 * <p>{@code String}-typed for the reason given on {@code PageQuery}: an uninterpretable filter is
 * ignored rather than rejected, so {@code ?minimumAmount=lots} returns the unfiltered collection instead
 * of a 400.
 */
@ParameterObject
public record TransactionFilterQuery(

        @Parameter(description = "`TRANSFER`, `DEPOSIT` or `WITHDRAWAL`.", example = "TRANSFER")
        String type,

        @Parameter(description = "`PENDING`, `COMPLETED` or `FAILED`.", example = "COMPLETED")
        String status,

        @Parameter(description = "Inclusive lower bound on `createdAt`, as an ISO-8601 instant.",
                example = "2026-01-01T00:00:00Z")
        String dateFrom,

        @Parameter(description = "Inclusive upper bound on `createdAt`, as an ISO-8601 instant.",
                example = "2026-12-31T23:59:59Z")
        String dateTo,

        @Parameter(description = "Inclusive lower bound on amount.", example = "10.00")
        String minimumAmount,

        @Parameter(description = "Inclusive upper bound on amount.", example = "1000.00")
        String maximumAmount) {

    public TransactionFilter toFilter() {
        return new TransactionFilter(
                QueryValues.text(type).orElse(null),
                QueryValues.text(status).orElse(null),
                QueryValues.instant(dateFrom).orElse(null),
                QueryValues.instant(dateTo).orElse(null),
                QueryValues.decimal(minimumAmount).orElse(null),
                QueryValues.decimal(maximumAmount).orElse(null));
    }
}
