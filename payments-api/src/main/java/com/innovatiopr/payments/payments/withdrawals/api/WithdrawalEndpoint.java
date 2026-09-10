package com.innovatiopr.payments.payments.withdrawals.api;

import com.innovatiopr.payments.accounts.AccountId;
import com.innovatiopr.payments.payments.deposits.api.CashMovementRequest;
import com.innovatiopr.payments.payments.deposits.api.CashMovementResourceAssembler;
import com.innovatiopr.payments.payments.deposits.application.CashMovementResult;
import com.innovatiopr.payments.payments.withdrawals.application.WithdrawMoneyCommand;
import com.innovatiopr.payments.payments.withdrawals.application.WithdrawMoneyHandler;
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
class WithdrawalEndpoint {

    private final WithdrawMoneyHandler withdraw;
    private final CashMovementResourceAssembler assembler;
    private final RequestValidator validator;

    WithdrawalEndpoint(WithdrawMoneyHandler withdraw, CashMovementResourceAssembler assembler,
                       RequestValidator validator) {
        this.withdraw = withdraw;
        this.assembler = assembler;
        this.validator = validator;
    }

    ServerResponse withdraw(ServerRequest request) throws Exception {
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

        Result<CashMovementResult> result = withdraw.handle(new WithdrawMoneyCommand(
                AccountId.of(accountId), body.amount(), body.currency().toUpperCase(Locale.ROOT),
                body.reference()));

        String baseUrl = ApiPaths.baseUrl(request);
        return ApiResponses.created(result,
                movement -> assembler.toResource(movement, baseUrl),
                movement -> URI.create(ApiPaths.transaction(baseUrl, movement.transactionId())));
    }
}
