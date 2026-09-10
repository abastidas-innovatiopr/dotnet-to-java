package com.innovatiopr.payments.accounts.opening.api;

import com.innovatiopr.payments.accounts.details.api.AccountResourceAssembler;
import com.innovatiopr.payments.accounts.details.application.AccountDetails;
import com.innovatiopr.payments.accounts.opening.application.OpenAccountCommand;
import com.innovatiopr.payments.accounts.opening.application.OpenAccountHandler;
import com.innovatiopr.payments.accounts.opening.application.OpenAccountResult;
import com.innovatiopr.payments.customers.CustomerId;
import com.innovatiopr.payments.shared.api.ApiPaths;
import com.innovatiopr.payments.shared.api.ApiResponses;
import com.innovatiopr.payments.shared.api.RequestValidator;
import com.innovatiopr.payments.shared.domain.Result;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.function.ServerRequest;
import org.springframework.web.servlet.function.ServerResponse;

import java.net.URI;
import java.util.Locale;
import java.util.Optional;

@Component
class AccountOpeningEndpoint {

    private final OpenAccountHandler openAccount;
    private final AccountResourceAssembler assembler;
    private final RequestValidator validator;

    AccountOpeningEndpoint(OpenAccountHandler openAccount, AccountResourceAssembler assembler,
                           RequestValidator validator) {
        this.openAccount = openAccount;
        this.assembler = assembler;
        this.validator = validator;
    }

    ServerResponse open(ServerRequest request) throws Exception {
        OpenAccountRequest body = request.body(OpenAccountRequest.class);

        Optional<ServerResponse> invalid = validator.validate(body);
        if (invalid.isPresent()) {
            return invalid.get();
        }

        Result<OpenAccountResult> result = openAccount.handle(new OpenAccountCommand(
                CustomerId.of(body.customerId()),
                body.currency().toUpperCase(Locale.ROOT)));

        String baseUrl = ApiPaths.baseUrl(request);
        return ApiResponses.created(result,
                opened -> assembler.toResource(toDetails(opened), baseUrl),
                opened -> URI.create(ApiPaths.account(baseUrl, opened.accountId())));
    }

    private static AccountDetails toDetails(OpenAccountResult result) {
        return new AccountDetails(result.accountId(), result.customerId(), result.accountNumber(),
                result.currency(), result.balance(), result.status(), result.openedAt());
    }
}
