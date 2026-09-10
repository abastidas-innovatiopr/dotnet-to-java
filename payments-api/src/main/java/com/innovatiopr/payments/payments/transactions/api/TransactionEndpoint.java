package com.innovatiopr.payments.payments.transactions.api;

import com.innovatiopr.payments.accounts.AccountId;
import com.innovatiopr.payments.payments.domain.TransactionId;
import com.innovatiopr.payments.payments.transactions.application.GetAccountTransactionsHandler;
import com.innovatiopr.payments.payments.transactions.application.GetTransactionHandler;
import com.innovatiopr.payments.payments.transactions.application.GetTransactionsHandler;
import com.innovatiopr.payments.payments.transactions.application.TransactionDetails;
import com.innovatiopr.payments.payments.transactions.application.TransactionFilter;
import com.innovatiopr.payments.payments.transactions.application.TransactionQueries;
import com.innovatiopr.payments.shared.api.ApiPaths;
import com.innovatiopr.payments.shared.api.ApiResponses;
import com.innovatiopr.payments.shared.api.PageParams;
import com.innovatiopr.payments.shared.api.PagedResources;
import com.innovatiopr.payments.shared.application.PageRequest;
import com.innovatiopr.payments.shared.application.PageResult;
import com.innovatiopr.payments.shared.domain.Result;
import org.springframework.hateoas.MediaTypes;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.function.ServerRequest;
import org.springframework.web.servlet.function.ServerResponse;

import java.util.List;
import java.util.UUID;

@Component
class TransactionEndpoint {

    private final GetTransactionHandler getTransaction;
    private final GetTransactionsHandler getTransactions;
    private final GetAccountTransactionsHandler getAccountTransactions;
    private final TransactionResourceAssembler assembler;

    TransactionEndpoint(GetTransactionHandler getTransaction, GetTransactionsHandler getTransactions,
                        GetAccountTransactionsHandler getAccountTransactions,
                        TransactionResourceAssembler assembler) {
        this.getTransaction = getTransaction;
        this.getTransactions = getTransactions;
        this.getAccountTransactions = getAccountTransactions;
        this.assembler = assembler;
    }

    ServerResponse getById(ServerRequest request) {
        UUID transactionId;
        try {
            transactionId = UUID.fromString(request.pathVariable("transactionId"));
        } catch (IllegalArgumentException e) {
            return ApiResponses.invalidIdentifier("transaction id", request.pathVariable("transactionId"));
        }

        String baseUrl = ApiPaths.baseUrl(request);
        return ApiResponses.ok(
                getTransaction.handle(new TransactionQueries.GetTransactionQuery(
                        TransactionId.of(transactionId))),
                details -> assembler.toResource(details, baseUrl));
    }

    ServerResponse list(ServerRequest request) {
        PageRequest page = PageParams.from(request);
        TransactionFilter filter = filterFrom(request);

        Result<PageResult<TransactionDetails>> result = getTransactions.handle(
                new TransactionQueries.GetTransactionsQuery(filter, page));

        String baseUrl = ApiPaths.baseUrl(request);
        return respondWithPage(result, request, ApiPaths.transactions(baseUrl), baseUrl);
    }

    ServerResponse listForAccount(ServerRequest request) {
        UUID accountId;
        try {
            accountId = UUID.fromString(request.pathVariable("accountId"));
        } catch (IllegalArgumentException e) {
            return ApiResponses.invalidIdentifier("account id", request.pathVariable("accountId"));
        }

        PageRequest page = PageParams.from(request);
        TransactionFilter filter = filterFrom(request);

        Result<PageResult<TransactionDetails>> result = getAccountTransactions.handle(
                new TransactionQueries.GetAccountTransactionsQuery(AccountId.of(accountId), filter, page));

        String baseUrl = ApiPaths.baseUrl(request);
        return respondWithPage(result, request, ApiPaths.accountTransactions(baseUrl, accountId), baseUrl);
    }

    private ServerResponse respondWithPage(Result<PageResult<TransactionDetails>> result,
                                           ServerRequest request, String collectionHref, String baseUrl) {
        return result.fold(
                found -> {
                    List<TransactionResource> resources = found.items().stream()
                            .map(details -> assembler.toResource(details, baseUrl))
                            .toList();
                    return ServerResponse.ok()
                            .contentType(MediaTypes.HAL_JSON)
                            .body(PagedResources.of(found, resources, request, collectionHref));
                },
                ApiResponses::problem);
    }

    private static TransactionFilter filterFrom(ServerRequest request) {
        return new TransactionFilter(
                PageParams.stringParam(request, "type").orElse(null),
                PageParams.stringParam(request, "status").orElse(null),
                PageParams.instantParam(request, "dateFrom").orElse(null),
                PageParams.instantParam(request, "dateTo").orElse(null),
                PageParams.decimalParam(request, "minimumAmount").orElse(null),
                PageParams.decimalParam(request, "maximumAmount").orElse(null));
    }
}
