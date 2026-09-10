package com.innovatiopr.payments.shared.api.docs;

import io.swagger.v3.oas.annotations.enums.ParameterIn;

import static org.springdoc.core.fn.builders.apiresponse.Builder.responseBuilder;
import static org.springdoc.core.fn.builders.content.Builder.contentBuilder;
import static org.springdoc.core.fn.builders.parameter.Builder.parameterBuilder;
import static org.springdoc.core.fn.builders.schema.Builder.schemaBuilder;

/**
 * Reusable OpenAPI fragments shared by every route declaration.
 *
 * <p>The pagination parameters, the {@code Idempotency-Key} header and the Problem Details responses are
 * described once here rather than restated in a dozen slices, so a change to the convention shows up
 * everywhere at once.
 *
 * <p>Note the static imports. springdoc names every one of its builder classes {@code Builder}
 * ({@code ...fn.builders.operation.Builder}, {@code ...parameter.Builder}, {@code ...schema.Builder}, and
 * so on), so importing more than one by simple name does not compile. Importing the static factory
 * methods instead keeps the call sites readable.
 */
public final class OpenApiDocs {

    public static final String TAG_CUSTOMERS = "Customers";
    public static final String TAG_ACCOUNTS = "Accounts";
    public static final String TAG_PAYMENTS = "Payments";
    public static final String TAG_TRANSACTIONS = "Transactions";
    public static final String TAG_LEDGER = "Ledger";

    private OpenApiDocs() {
    }

    /* ------------------------------------------------------------------ parameters */

    public static org.springdoc.core.fn.builders.parameter.Builder uuidPathParam(String name,
                                                                                 String description) {
        return parameterBuilder()
                .name(name)
                .in(ParameterIn.PATH)
                .required(true)
                .description(description)
                .schema(schemaBuilder().type("string").format("uuid"));
    }

    public static org.springdoc.core.fn.builders.parameter.Builder queryParam(String name, String description,
                                                                              String type, String example) {
        return parameterBuilder()
                .name(name)
                .in(ParameterIn.QUERY)
                .required(false)
                .description(description)
                .example(example)
                .schema(schemaBuilder().type(type));
    }

    public static org.springdoc.core.fn.builders.parameter.Builder idempotencyKeyHeader() {
        return parameterBuilder()
                .name("Idempotency-Key")
                .in(ParameterIn.HEADER)
                .required(true)
                .description("Client-generated key that makes this request safe to retry. Resend the SAME "
                        + "key after a timeout: the server guarantees the money moves at most once. "
                        + "Replaying the key with an identical body returns the original result with 200; "
                        + "replaying it with a different body returns 409. 8-255 characters of letters, "
                        + "digits, '.', '_', ':' or '-' - a UUID is a good choice.")
                .example("6a1f4c2e-9d3b-4f1a-8c7e-2b5d0a9f3e11")
                .schema(schemaBuilder().type("string"));
    }

    /** Adds {@code page}, {@code size}, {@code sort} and {@code direction}. */
    public static org.springdoc.core.fn.builders.operation.Builder withPagination(
            org.springdoc.core.fn.builders.operation.Builder operation,
            String sortableFields, String defaultSort) {
        return operation
                .parameter(queryParam("page",
                        "Zero-based page number. Negative values normalise to 0.", "integer", "0"))
                .parameter(queryParam("size",
                        "Page size. Default 20, maximum 100 - larger values are clamped and the effective "
                                + "size is echoed back in the response.", "integer", "20"))
                .parameter(queryParam("sort",
                        "Sort field. Allowed: " + sortableFields + ". Unknown values fall back to "
                                + defaultSort + ".", "string", defaultSort))
                .parameter(queryParam("direction", "asc or desc.", "string", "desc"));
    }

    /* ------------------------------------------------------------------ responses */

    public static org.springdoc.core.fn.builders.apiresponse.Builder hal(String status, String description) {
        return responseBuilder()
                .responseCode(status)
                .description(description)
                .content(contentBuilder().mediaType("application/hal+json"));
    }

    public static org.springdoc.core.fn.builders.apiresponse.Builder problem(String status,
                                                                             String description) {
        return responseBuilder()
                .responseCode(status)
                .description(description)
                .content(contentBuilder().mediaType("application/problem+json"));
    }

    public static org.springdoc.core.fn.builders.operation.Builder withNotFound(
            org.springdoc.core.fn.builders.operation.Builder operation, String what) {
        return operation.response(problem("404", what + " does not exist."));
    }

    public static org.springdoc.core.fn.builders.operation.Builder withValidation(
            org.springdoc.core.fn.builders.operation.Builder operation) {
        return operation.response(problem("400",
                "The request body or a path variable failed transport validation. The `errors` array "
                        + "names each offending field."));
    }
}
