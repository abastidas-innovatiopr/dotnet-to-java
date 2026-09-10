package com.innovatiopr.payments.payments.transfer.api;

import com.innovatiopr.payments.shared.api.ApiPaths;
import com.innovatiopr.payments.shared.api.docs.OpenApiDocs;
import org.springdoc.core.fn.builders.operation.Builder;
import org.springdoc.webmvc.core.fn.SpringdocRouteBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.ServerResponse;

import static org.springdoc.core.fn.builders.requestbody.Builder.requestBodyBuilder;

/**
 * Routes for the transfer feature.
 *
 * <p>Compare with ASP.NET Core Minimal APIs:
 * <pre>{@code
 * // .NET
 * app.MapPost("/api/v1/transfers", TransferMoney);
 *
 * // Spring
 * SpringdocRouteBuilder.route().POST("/api/v1/transfers", endpoint::transfer, ops -> ...).build();
 * }</pre>
 *
 * <h2>Why SpringdocRouteBuilder rather than RouterFunctions</h2>
 * springdoc discovers annotated controllers by reflection. A {@code RouterFunction} is an opaque object
 * built at runtime, so the generated OpenAPI document listed <em>no paths at all</em> — and Scalar
 * rendered an empty API reference. That is the third real cost of functional routing, after losing
 * {@code linkTo(methodOn(...))} and automatic {@code @Valid}.
 *
 * <p>{@code SpringdocRouteBuilder} is a drop-in replacement for {@code RouterFunctions.route()} that
 * takes an extra operation-builder argument per route. Documentation is declared in the same call as the
 * route, so the two cannot drift: delete a route and its documentation goes with it.
 *
 * <p>{@code @Transactional} must never appear on a {@code RouterFunction} bean or a handler function. The
 * bean is built once at startup, and the handler is a method reference invoked by the
 * {@code DispatcherServlet} rather than through a Spring proxy — so the annotation would be silently
 * inert. Transactions belong on the application handlers.
 */
@Configuration(proxyBeanMethods = false)
class TransferRoutes {

    @Bean
    RouterFunction<ServerResponse> transferRouterFunction(TransferEndpoint endpoint) {
        return SpringdocRouteBuilder.route()
                .POST(ApiPaths.TRANSFERS, endpoint::transfer, TransferRoutes::transferDocs)
                .build();
    }

    private static void transferDocs(Builder operation) {
        operation
                .operationId("transferMoney")
                .tags(new String[]{OpenApiDocs.TAG_PAYMENTS})
                .summary("Transfer money between two accounts")
                .description("""
                        The application's primary workflow. Source balance, destination balance, the payment \
                        transaction, the balanced ledger postings and the idempotency record all commit in \
                        **one database transaction**.

                        Both accounts are locked with `SELECT ... FOR UPDATE` in a deterministic ascending \
                        id order, which prevents both double spending and deadlocks between opposing \
                        transfers.

                        Requires an `Idempotency-Key`. A retried request never moves money twice, even when \
                        the retries arrive concurrently.""")
                .parameter(OpenApiDocs.idempotencyKeyHeader())
                .requestBody(requestBodyBuilder()
                        .required(true)
                        .implementation(TransferRequest.class)
                        .description("Source, destination, amount, ISO-4217 currency and an optional reference."))
                .response(OpenApiDocs.hal("201", "Transfer completed. Carries both resulting balances and a "
                        + "`Location` header pointing at the transaction."))
                .response(OpenApiDocs.hal("200", "Replayed from a stored idempotency record. Same body as the "
                        + "original response with `replayed: true`; no money moved."))
                .response(OpenApiDocs.problem("400", "Missing or malformed `Idempotency-Key`, or an invalid body."))
                .response(OpenApiDocs.problem("404", "One of the accounts does not exist."))
                .response(OpenApiDocs.problem("409",
                        "`TRANSFER_IDEMPOTENCY_KEY_REUSED` - the key was already used for a different request."))
                .response(OpenApiDocs.problem("422",
                        "A domain rule refused the transfer: `ACCOUNT_INSUFFICIENT_FUNDS`, `ACCOUNT_FROZEN`, "
                                + "`ACCOUNT_CLOSED`, `TRANSFER_SAME_ACCOUNT` or `MONEY_CURRENCY_MISMATCH`."));
    }
}
