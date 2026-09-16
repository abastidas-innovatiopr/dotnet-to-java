package com.innovatiopr.payments.payments.transactions.api;

import com.innovatiopr.payments.accounts.AccountId;
import com.innovatiopr.payments.payments.domain.TransactionId;
import com.innovatiopr.payments.payments.transactions.application.GetAccountTransactionsHandler;
import com.innovatiopr.payments.payments.transactions.application.GetTransactionHandler;
import com.innovatiopr.payments.payments.transactions.application.GetTransactionsHandler;
import com.innovatiopr.payments.payments.transactions.application.TransactionDetails;
import com.innovatiopr.payments.payments.transactions.application.TransactionQueries;
import com.innovatiopr.payments.shared.api.ApiPaths;
import com.innovatiopr.payments.shared.api.PageQuery;
import com.innovatiopr.payments.shared.api.PagedResources;
import com.innovatiopr.payments.shared.api.docs.OpenApiDocs;
import com.innovatiopr.payments.shared.application.PageResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.hateoas.MediaTypes;
import org.springframework.hateoas.PagedModel;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * Transaction history.
 *
 * <p>Mapped at the API root rather than at one collection, because this slice owns paths under both
 * {@code /transactions} and {@code /accounts/{id}/transactions}. Splitting it in two to give each a
 * tidier class-level mapping would divide one vertical slice across two files for no gain.
 */
@RestController
@RequestMapping(path = ApiPaths.API_V1, produces = MediaTypes.HAL_JSON_VALUE)
@Tag(name = OpenApiDocs.TAG_TRANSACTIONS)
public class TransactionController {

    private static final String DEFAULT_SORT = "createdAt";

    private final GetTransactionHandler getTransaction;
    private final GetTransactionsHandler getTransactions;
    private final GetAccountTransactionsHandler getAccountTransactions;
    private final TransactionResourceAssembler assembler;

    TransactionController(GetTransactionHandler getTransaction, GetTransactionsHandler getTransactions,
                          GetAccountTransactionsHandler getAccountTransactions,
                          TransactionResourceAssembler assembler) {
        this.getTransaction = getTransaction;
        this.getTransactions = getTransactions;
        this.getAccountTransactions = getAccountTransactions;
        this.assembler = assembler;
    }

    @GetMapping("/transactions/{transactionId}")
    @Operation(operationId = "getTransaction", summary = "Fetch one transaction")
    @ApiResponse(responseCode = "200", description = "The transaction, linked to the accounts involved.")
    @ApiResponse(responseCode = "404", description = "`TRANSACTION_NOT_FOUND`.")
    public TransactionResource getById(
            @PathVariable @Parameter(description = OpenApiDocs.TRANSACTION_ID_DESCRIPTION)
            UUID transactionId) {
        return assembler.toResource(
                getTransaction.handle(new TransactionQueries.GetTransactionQuery(
                        TransactionId.of(transactionId))),
                ApiPaths.baseUrl());
    }

    @GetMapping("/transactions")
    @Operation(
            operationId = "listTransactions",
            summary = "List transactions",
            description = """
                    Paginated and filterable transaction history across all accounts.

                    Filters are applied to **both** the page and its total count, so the reported \
                    `totalPages` always describes the filtered set. They are also carried into the \
                    navigation links, so following `next` continues the same filtered collection.

                    Served by a `JdbcClient` read model, not by loading aggregates.

                    Sortable fields: `createdAt`, `amount`, `status`. Unknown values fall back to \
                    `createdAt`.""")
    @ApiResponse(responseCode = "200", description = "A page of transactions with `page` metadata and links.")
    public PagedModel<TransactionResource> list(PageQuery page, TransactionFilterQuery filter) {
        String baseUrl = ApiPaths.baseUrl();
        PageResult<TransactionDetails> found = getTransactions.handle(
                new TransactionQueries.GetTransactionsQuery(
                        filter.toFilter(), page.toPageRequest(DEFAULT_SORT)));
        return toPage(found, ApiPaths.transactions(baseUrl), baseUrl);
    }

    @GetMapping("/accounts/{accountId}/transactions")
    @Operation(
            operationId = "listAccountTransactions",
            summary = "List one account's transactions",
            description = """
                    History for a single account, matching it as either the source or the destination.

                    Returns **404 rather than an empty page** when the account does not exist: \
                    "no rows" and "no such account" are different answers and a client needs to tell \
                    them apart.

                    Sortable fields: `createdAt`, `amount`, `status`. Unknown values fall back to \
                    `createdAt`.""")
    @ApiResponse(responseCode = "200", description = "A page of transactions for this account.")
    @ApiResponse(responseCode = "404", description = "`ACCOUNT_NOT_FOUND`.")
    public PagedModel<TransactionResource> listForAccount(
            @PathVariable @Parameter(description = OpenApiDocs.ACCOUNT_ID_DESCRIPTION) UUID accountId,
            PageQuery page,
            TransactionFilterQuery filter) {

        String baseUrl = ApiPaths.baseUrl();
        PageResult<TransactionDetails> found = getAccountTransactions.handle(
                new TransactionQueries.GetAccountTransactionsQuery(
                        AccountId.of(accountId), filter.toFilter(), page.toPageRequest(DEFAULT_SORT)));
        return toPage(found, ApiPaths.accountTransactions(baseUrl, accountId), baseUrl);
    }

    private PagedModel<TransactionResource> toPage(PageResult<TransactionDetails> found,
                                                   String collectionHref, String baseUrl) {
        List<TransactionResource> resources = found.items().stream()
                .map(details -> assembler.toResource(details, baseUrl))
                .toList();
        return PagedResources.of(found, resources, collectionHref);
    }
}
