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
 * OpenAPI metadata.
 *
 * <h2>A caveat about functional endpoints</h2>
 * springdoc discovers annotated controllers by reflection. A {@code RouterFunction} is an opaque object
 * built at runtime, so springdoc cannot see the routes registered here and the generated document lists
 * no paths by default. This is the third real cost of functional routing, after losing
 * {@code linkTo(methodOn(...))} and automatic {@code @Valid}.
 *
 * <p>The fix is {@code @RouterOperation}/{@code @RouterOperations} from springdoc, or a hand-written
 * document. This class supplies the API-level metadata and the reusable components — the
 * {@code Idempotency-Key} header, the Problem Details schema, the pagination parameters — that route
 * documentation refers to, and {@code docs/API.md} carries the full endpoint reference.
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
