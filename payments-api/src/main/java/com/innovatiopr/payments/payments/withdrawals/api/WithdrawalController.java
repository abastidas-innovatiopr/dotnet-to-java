package com.innovatiopr.payments.payments.withdrawals.api;

import com.innovatiopr.payments.accounts.AccountId;
import com.innovatiopr.payments.payments.deposits.api.CashMovementRequest;
import com.innovatiopr.payments.payments.deposits.api.CashMovementResource;
import com.innovatiopr.payments.payments.deposits.api.CashMovementResourceAssembler;
import com.innovatiopr.payments.payments.deposits.application.CashMovementResult;
import com.innovatiopr.payments.payments.withdrawals.application.WithdrawMoneyCommand;
import com.innovatiopr.payments.payments.withdrawals.application.WithdrawMoneyHandler;
import com.innovatiopr.payments.shared.api.ApiPaths;
import com.innovatiopr.payments.shared.api.ApiResponses;
import com.innovatiopr.payments.shared.api.docs.OpenApiDocs;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.hateoas.MediaTypes;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.Locale;
import java.util.UUID;

@RestController
@RequestMapping(path = ApiPaths.ACCOUNTS, produces = MediaTypes.HAL_JSON_VALUE)
@Tag(name = OpenApiDocs.TAG_PAYMENTS)
public class WithdrawalController {

    private final WithdrawMoneyHandler withdraw;
    private final CashMovementResourceAssembler assembler;

    WithdrawalController(WithdrawMoneyHandler withdraw, CashMovementResourceAssembler assembler) {
        this.withdraw = withdraw;
        this.assembler = assembler;
    }

    @PostMapping("/{accountId}/withdrawals")
    @Operation(
            operationId = "withdrawMoney",
            summary = "Withdraw money from an account",
            description = """
                    Cash out of the system. Debits the account and credits the settlement position.

                    The account row is locked for the duration, so concurrent withdrawals cannot both \
                    pass the sufficient-funds check.""")
    @ApiResponse(responseCode = "201", description = "Withdrawal recorded. Carries the resulting balance.")
    @ApiResponse(responseCode = "400", description = "Invalid body or an unrecognised currency.")
    @ApiResponse(responseCode = "404", description = "`ACCOUNT_NOT_FOUND`.")
    @ApiResponse(responseCode = "422",
            description = "`ACCOUNT_INSUFFICIENT_FUNDS`, `ACCOUNT_FROZEN`, `ACCOUNT_CLOSED` or "
                    + "`MONEY_CURRENCY_MISMATCH`.")
    public ResponseEntity<CashMovementResource> withdraw(
            @PathVariable @Parameter(description = "Account to debit.") UUID accountId,
            @Valid @RequestBody CashMovementRequest body) {

        CashMovementResult movement = withdraw.handle(new WithdrawMoneyCommand(
                AccountId.of(accountId), body.amount(), body.currency().toUpperCase(Locale.ROOT),
                body.reference()));

        String baseUrl = ApiPaths.baseUrl();
        return ApiResponses.created(
                assembler.toResource(movement, baseUrl),
                URI.create(ApiPaths.transaction(baseUrl, movement.transactionId())));
    }
}
