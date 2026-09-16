package com.innovatiopr.payments.shared.api.docs;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.media.ArraySchema;
import io.swagger.v3.oas.models.media.IntegerSchema;
import io.swagger.v3.oas.models.media.ObjectSchema;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * API-level OpenAPI metadata: title, version, and the description Scalar renders as its introduction.
 *
 * <h2>Per-operation documentation lives on the controller method</h2>
 * springdoc discovers annotated controllers by reflection, so paths, request schemas and parameter types
 * are generated from the signature and only the prose has to be written. Each method still declares an
 * explicit {@code @Operation(operationId = ...)}: springdoc would otherwise derive the id from the Java
 * method name and produce {@code getById}, {@code getById_1} and so on, silently renaming every operation
 * in a generated client. That is the one piece of route metadata that cannot be left to inference.
 *
 * <p>Fragments shared across slices — the {@code Idempotency-Key} header text, the pagination parameters
 * and the common descriptions — live in {@link OpenApiDocs}, and the media type on each response is
 * applied by {@link PaymentsOperationCustomizer}.
 *
 * <p>{@code OpenApiDocumentIT} asserts that every route reaches the document and that each carries its
 * expected {@code operationId}, so a renamed method or a forgotten annotation fails the build rather than
 * silently reshaping the API reference.
 */
@Configuration(proxyBeanMethods = false)
class OpenApiConfiguration {

    @Bean
    OpenAPI paymentsOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Payments API")
                        .version("1.0.0")
                        .description("""
                                A production-style payments API built with Domain-Driven Design, Clean
                                Architecture, vertical slices, CQRS and a modular monolith.

                                ## Conventions

                                **Pagination** — every collection is paginated. `page` is zero-based and
                                `size` defaults to 20 and is capped at 100. Out-of-range values are
                                normalised rather than rejected, and the effective values are echoed in
                                the `page` object of the response.

                                **Sorting** — `sort` names a whitelisted logical field and `direction` is
                                `asc` or `desc` (default `desc`). Unknown fields fall back to the
                                collection's default. Every ordering includes a unique tiebreaker so paging
                                is deterministic.

                                **Filtering** — transaction history accepts `type`, `status`, `dateFrom`,
                                `dateTo`, `minimumAmount` and `maximumAmount`. Filters are applied to both
                                the page and its total count, and are preserved in the navigation links.

                                **Hypermedia** — responses are `application/hal+json` and carry `_links`.
                                Action links reflect current domain state: a FROZEN account advertises no
                                `deposit` or `withdraw` link, and a CLOSED account advertises none at all.

                                **Idempotency** — `POST /api/v1/transfers` requires an `Idempotency-Key`
                                header. Replaying the key with the same body returns the original result
                                with `200 OK`; replaying it with a different body returns `409 Conflict`.

                                **Errors** — failures are RFC 9457 Problem Details
                                (`application/problem+json`) carrying a stable machine-readable `code`.
                                `400` malformed request, `404` not found, `409` conflict,
                                `422` domain rule violated, `500` unexpected.
                                """)
                        .contact(new Contact().name("Payments Platform"))
                        .license(new License().name("MIT")))
                .components(new Components()
                        .addSchemas("ProblemDetail", problemDetailSchema()));
    }

    /**
     * The RFC 9457 body every failure carries, declared once and referenced by
     * {@link PaymentsOperationCustomizer} from every 4xx and 5xx response.
     *
     * <p>Written out by hand rather than derived from Spring's {@code ProblemDetail} class, because the
     * interesting fields are the extensions this application adds — {@code code}, {@code errors} and
     * {@code correlationId} — and those live in a property map that no schema generator can see.
     */
    private static Schema<?> problemDetailSchema() {
        return new ObjectSchema()
                .description("RFC 9457 problem document.")
                .addProperty("type", new StringSchema()
                        .description("Stable URI identifying the problem kind.")
                        .example("https://api.payments.local/problems/account_insufficient_funds"))
                .addProperty("title", new StringSchema().example("Business rule violated"))
                .addProperty("status", new IntegerSchema().example(422))
                .addProperty("detail", new StringSchema()
                        .description("Human-readable explanation. Never contains secrets."))
                .addProperty("instance", new StringSchema().example("/api/v1/transfers"))
                .addProperty("code", new StringSchema()
                        .description("Machine-readable identifier. Part of the published contract - "
                                + "branch on this, never on `detail`.")
                        .example("ACCOUNT_INSUFFICIENT_FUNDS"))
                .addProperty("errors", new ArraySchema()
                        .description("Present only when a request body failed Bean Validation: one entry "
                                + "per offending field, sorted by field name.")
                        .items(new ObjectSchema()
                                .addProperty("field", new StringSchema().example("email"))
                                .addProperty("message", new StringSchema()
                                        .example("Email must be a valid address"))))
                .addProperty("correlationId", new StringSchema()
                        .description("Echoed from the request so a client can quote it in a support "
                                + "ticket and an operator can find the request in the logs."))
                .addProperty("traceId", new StringSchema().description("Distributed-tracing identifier."))
                .addRequiredItem("type")
                .addRequiredItem("title")
                .addRequiredItem("status")
                .addRequiredItem("code");
    }
}
