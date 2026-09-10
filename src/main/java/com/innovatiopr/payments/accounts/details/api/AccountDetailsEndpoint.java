package com.innovatiopr.payments.accounts.details.api;

import com.innovatiopr.payments.accounts.AccountId;
import com.innovatiopr.payments.accounts.details.application.GetAccountHandler;
import com.innovatiopr.payments.accounts.details.application.GetAccountQuery;
import com.innovatiopr.payments.accounts.details.application.GetBalanceHandler;
import com.innovatiopr.payments.accounts.details.application.GetBalanceQuery;
import com.innovatiopr.payments.shared.api.ApiPaths;
import com.innovatiopr.payments.shared.api.ApiResponses;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.function.ServerRequest;
import org.springframework.web.servlet.function.ServerResponse;

import java.util.UUID;

@Component
class AccountDetailsEndpoint {

    private final GetAccountHandler getAccount;
    private final GetBalanceHandler getBalance;
    private final AccountResourceAssembler assembler;

    AccountDetailsEndpoint(GetAccountHandler getAccount, GetBalanceHandler getBalance,
                           AccountResourceAssembler assembler) {
        this.getAccount = getAccount;
        this.getBalance = getBalance;
        this.assembler = assembler;
    }

    ServerResponse getById(ServerRequest request) {
        UUID accountId;
        try {
            accountId = UUID.fromString(request.pathVariable("accountId"));
        } catch (IllegalArgumentException e) {
            return ApiResponses.invalidIdentifier("account id", request.pathVariable("accountId"));
        }

        String baseUrl = ApiPaths.baseUrl(request);
        return ApiResponses.ok(getAccount.handle(new GetAccountQuery(AccountId.of(accountId))),
                details -> assembler.toResource(details, baseUrl));
    }

    ServerResponse getBalance(ServerRequest request) {
        UUID accountId;
        try {
            accountId = UUID.fromString(request.pathVariable("accountId"));
        } catch (IllegalArgumentException e) {
            return ApiResponses.invalidIdentifier("account id", request.pathVariable("accountId"));
        }

        String baseUrl = ApiPaths.baseUrl(request);
        return ApiResponses.ok(getBalance.handle(new GetBalanceQuery(AccountId.of(accountId))),
                balance -> assembler.toBalanceResource(balance, baseUrl));
    }
}
