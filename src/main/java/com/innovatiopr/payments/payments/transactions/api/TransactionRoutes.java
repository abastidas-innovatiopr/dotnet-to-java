package com.innovatiopr.payments.payments.transactions.api;

import com.innovatiopr.payments.shared.api.ApiPaths;
import com.innovatiopr.payments.shared.api.docs.OpenApiDocs;
import org.springdoc.core.fn.builders.operation.Builder;
import org.springdoc.webmvc.core.fn.SpringdocRouteBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.ServerResponse;

@Configuration(proxyBeanMethods = false)
class TransactionRoutes {

    @Bean
    RouterFunction<ServerResponse> transactionRouterFunction(TransactionEndpoint endpoint) {
        return SpringdocRouteBuilder.route()
                .GET(ApiPaths.ACCOUNT_TRANSACTIONS, endpoint::listForAccount,
                        TransactionRoutes::accountHistoryDocs)
                .GET(ApiPaths.TRANSACTION, endpoint::getById, TransactionRoutes::getDocs)
                .GET(ApiPaths.TRANSACTIONS, endpoint::list, TransactionRoutes::listDocs)
                .build();
    }

    private static void getDocs(Builder operation) {
        operation
                .operationId("getTransaction")
                .tags(new String[]{OpenApiDocs.TAG_TRANSACTIONS})
                .summary("Fetch one transaction")
                .parameter(OpenApiDocs.uuidPathParam("transactionId", "Transaction identifier."))
                .response(OpenApiDocs.hal("200", "The transaction, linked to the accounts involved."))
                .response(OpenApiDocs.problem("404", "`TRANSACTION_NOT_FOUND`."));
    }

    private static void listDocs(Builder operation) {
        withFilters(OpenApiDocs.withPagination(operation
                .operationId("listTransactions")
                .tags(new String[]{OpenApiDocs.TAG_TRANSACTIONS})
                .summary("List transactions")
                .description("""
                        Paginated and filterable transaction history across all accounts.

                        Filters are applied to **both** the page and its total count, so the reported \
                        `totalPages` always describes the filtered set. They are also carried into the \
                        navigation links, so following `next` continues the same filtered collection.

                        Served by a `JdbcClient` read model, not by loading aggregates.""")
                .response(OpenApiDocs.hal("200", "A page of transactions with `page` metadata and links.")),
                "`createdAt`, `amount`, `status`", "createdAt"));
    }

    private static void accountHistoryDocs(Builder operation) {
        withFilters(OpenApiDocs.withPagination(operation
                .operationId("listAccountTransactions")
                .tags(new String[]{OpenApiDocs.TAG_TRANSACTIONS})
                .summary("List one account's transactions")
                .description("""
                        History for a single account, matching it as either the source or the destination.

                        Returns **404 rather than an empty page** when the account does not exist: \
                        "no rows" and "no such account" are different answers and a client needs to tell \
                        them apart.""")
                .parameter(OpenApiDocs.uuidPathParam("accountId", "Account identifier."))
                .response(OpenApiDocs.hal("200", "A page of transactions for this account."))
                .response(OpenApiDocs.problem("404", "`ACCOUNT_NOT_FOUND`.")),
                "`createdAt`, `amount`, `status`", "createdAt"));
    }

    /** The transaction-history filters, shared by both collection endpoints. */
    private static Builder withFilters(Builder operation) {
        return operation
                .parameter(OpenApiDocs.queryParam("type",
                        "`TRANSFER`, `DEPOSIT` or `WITHDRAWAL`.", "string", "TRANSFER"))
                .parameter(OpenApiDocs.queryParam("status",
                        "`PENDING`, `COMPLETED` or `FAILED`.", "string", "COMPLETED"))
                .parameter(OpenApiDocs.queryParam("dateFrom",
                        "Inclusive lower bound on `createdAt`, as an ISO-8601 instant.",
                        "string", "2026-01-01T00:00:00Z"))
                .parameter(OpenApiDocs.queryParam("dateTo",
                        "Inclusive upper bound on `createdAt`, as an ISO-8601 instant.",
                        "string", "2026-12-31T23:59:59Z"))
                .parameter(OpenApiDocs.queryParam("minimumAmount",
                        "Inclusive lower bound on amount.", "number", "10.00"))
                .parameter(OpenApiDocs.queryParam("maximumAmount",
                        "Inclusive upper bound on amount.", "number", "1000.00"));
    }
}
