package com.innovatiopr.payments.shared.api.docs;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.parameters.HeaderParameter;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.media.StringSchema;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * API-level OpenAPI metadata: title, version, and the description Scalar renders as its introduction.
 *
 * <h2>Per-route documentation lives with the routes</h2>
 * springdoc discovers annotated controllers by reflection, and a {@code RouterFunction} is an opaque
 * object built at runtime — so by default the generated document contains <em>no paths at all</em> and a
 * documentation UI renders a blank page while everything appears to work. That is the third real cost of
 * functional routing, after losing {@code linkTo(methodOn(...))} and automatic {@code @Valid}.
 *
 * <p>Each route therefore declares its own operation through springdoc's {@code SpringdocRouteBuilder}
 * (see {@code TransferRoutes}), which keeps the documentation in the same call as the route so the two
 * cannot drift. Fragments shared across slices — the {@code Idempotency-Key} header, the pagination
 * parameters, the HAL and Problem Details responses — live in {@link OpenApiDocs}.
 *
 * <p>{@code OpenApiDocumentIT} asserts that every route reaches the document, so adding one with plain
 * {@code RouterFunctions.route()} fails the build rather than silently shrinking the API reference.
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
                        .addParameters("IdempotencyKey", idempotencyKeyHeader())
                        .addParameters("Page", queryParam("page", "Zero-based page number", "0"))
                        .addParameters("Size", queryParam("size", "Page size (default 20, maximum 100)", "20"))
                        .addParameters("Sort", queryParam("sort", "Whitelisted sort field", "createdAt"))
                        .addParameters("Direction", queryParam("direction", "asc or desc", "desc")));
    }

    private static Parameter idempotencyKeyHeader() {
        return new HeaderParameter()
                .name("Idempotency-Key")
                .required(true)
                .description("""
                        Client-generated key that makes this request safe to retry. Use the same key when
                        resending after a timeout: the server guarantees the money moves at most once.
                        8-255 characters of letters, digits, '.', '_', ':' or '-'. A UUID is a good choice.
                        """)
                .schema(new StringSchema().minLength(8).maxLength(255))
                .example("6a1f4c2e-9d3b-4f1a-8c7e-2b5d0a9f3e11");
    }

    private static Parameter queryParam(String name, String description, String example) {
        return new Parameter()
                .in("query")
                .name(name)
                .required(false)
                .description(description)
                .schema(new StringSchema())
                .example(example);
    }
}
