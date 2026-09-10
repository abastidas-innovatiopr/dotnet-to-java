package com.innovatiopr.payments.payments.deposits.api;

import com.innovatiopr.payments.accounts.AccountId;
import com.innovatiopr.payments.payments.deposits.application.CashMovementResult;
import com.innovatiopr.payments.payments.deposits.application.DepositMoneyCommand;
import com.innovatiopr.payments.payments.deposits.application.DepositMoneyHandler;
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
import java.util.UUID;

@Component
class DepositEndpoint {

    private final DepositMoneyHandler deposit;
    private final CashMovementResourceAssembler assembler;
    private final RequestValidator validator;

    DepositEndpoint(DepositMoneyHandler deposit, CashMovementResourceAssembler assembler,
                    RequestValidator validator) {
        this.deposit = deposit;
        this.assembler = assembler;
        this.validator = validator;
    }

    ServerResponse deposit(ServerRequest request) throws Exception {
        UUID accountId;
        try {
            accountId = UUID.fromString(request.pathVariable("accountId"));
        } catch (IllegalArgumentException e) {
            return ApiResponses.invalidIdentifier("account id", request.pathVariable("accountId"));
        }

        CashMovementRequest body = request.body(CashMovementRequest.class);
        Optional<ServerResponse> invalid = validator.validate(body);
        if (invalid.isPresent()) {
            return invalid.get();
        }

        Result<CashMovementResult> result = deposit.handle(new DepositMoneyCommand(
                AccountId.of(accountId), body.amount(), body.currency().toUpperCase(Locale.ROOT),
                body.reference()));

        String baseUrl = ApiPaths.baseUrl(request);
        return ApiResponses.created(result,
                movement -> assembler.toResource(movement, baseUrl),
                movement -> URI.create(ApiPaths.transaction(baseUrl, movement.transactionId())));
    }
}
