package com.innovatiopr.payments.ledger.statements.api;

import com.innovatiopr.payments.shared.api.ApiPaths;
import com.innovatiopr.payments.shared.api.docs.OpenApiDocs;
import org.springdoc.core.fn.builders.operation.Builder;
import org.springdoc.webmvc.core.fn.SpringdocRouteBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.ServerResponse;

@Configuration(proxyBeanMethods = false)
class StatementRoutes {

    @Bean
    RouterFunction<ServerResponse> statementRouterFunction(StatementEndpoint endpoint) {
        return SpringdocRouteBuilder.route()
                .GET(ApiPaths.ACCOUNT_STATEMENT, endpoint::getStatement, StatementRoutes::statementDocs)
                .build();
    }

    private static void statementDocs(Builder operation) {
        OpenApiDocs.withPagination(operation
                .operationId("getAccountStatement")
                .tags(new String[]{OpenApiDocs.TAG_LEDGER})
                .summary("Fetch an account statement")
                .description("""
                        The auditable ledger view: one line per double-entry posting against this account.

                        Each line carries a `runningBalance` computed by a SQL **window function** over the \
                        account's whole history — it cannot be derived from the page alone, since page \
                        three still needs the balance carried forward from every earlier posting. The \
                        newest line's running balance always equals the account balance.

                        This is the clearest case in the codebase for `JdbcClient` over JPA: through the \
                        write model the same answer would mean loading every ledger entry into memory on \
                        every request.""")
                .parameter(OpenApiDocs.uuidPathParam("accountId", "Account identifier."))
                .parameter(OpenApiDocs.queryParam("from",
                        "Inclusive lower bound on `recordedAt`, as an ISO-8601 instant.",
                        "string", "2026-01-01T00:00:00Z"))
                .parameter(OpenApiDocs.queryParam("to",
                        "Inclusive upper bound on `recordedAt`, as an ISO-8601 instant.",
                        "string", "2026-12-31T23:59:59Z"))
                .response(OpenApiDocs.hal("200", "A page of statement lines, newest first."))
                .response(OpenApiDocs.problem("404", "`ACCOUNT_NOT_FOUND`.")),
                "`recordedAt`, `amount`", "recordedAt");
    }
}
