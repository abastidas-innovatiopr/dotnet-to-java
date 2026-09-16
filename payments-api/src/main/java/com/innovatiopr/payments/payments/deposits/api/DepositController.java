package com.innovatiopr.payments.payments.deposits.api;

import com.innovatiopr.payments.accounts.AccountId;
import com.innovatiopr.payments.payments.deposits.application.CashMovementResult;
import com.innovatiopr.payments.payments.deposits.application.DepositMoneyCommand;
import com.innovatiopr.payments.payments.deposits.application.DepositMoneyHandler;
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

/**
 * Deposits.
 *
 * <p>The path sits under {@code /accounts/{id}} because that is the right resource hierarchy for a
 * client, while the code lives in the Payments module because that is the right ownership for a financial
 * operation. URL shape and module ownership are separate decisions.
 */
@RestController
@RequestMapping(path = ApiPaths.ACCOUNTS, produces = MediaTypes.HAL_JSON_VALUE)
@Tag(name = OpenApiDocs.TAG_PAYMENTS)
public class DepositController {

    private final DepositMoneyHandler deposit;
    private final CashMovementResourceAssembler assembler;

    DepositController(DepositMoneyHandler deposit, CashMovementResourceAssembler assembler) {
        this.deposit = deposit;
        this.assembler = assembler;
    }

    @PostMapping("/{accountId}/deposits")
    @Operation(
            operationId = "depositMoney",
            summary = "Deposit money into an account",
            description = """
                    Cash in from outside the system. Credits the account and debits the settlement \
                    position, so the ledger stays balanced — double entry only works if every posting \
                    has a counterparty.

                    This is also how a newly opened account gets its opening balance.""")
    @ApiResponse(responseCode = "201", description = "Deposit recorded. Carries the resulting balance.")
    @ApiResponse(responseCode = "400", description = "Invalid body or an unrecognised currency.")
    @ApiResponse(responseCode = "404", description = "`ACCOUNT_NOT_FOUND`.")
    @ApiResponse(responseCode = "422",
            description = "`ACCOUNT_FROZEN`, `ACCOUNT_CLOSED` or `MONEY_CURRENCY_MISMATCH`.")
    public ResponseEntity<CashMovementResource> deposit(
            @PathVariable @Parameter(description = "Account to credit.") UUID accountId,
            @Valid @RequestBody CashMovementRequest body) {

        CashMovementResult movement = deposit.handle(new DepositMoneyCommand(
                AccountId.of(accountId), body.amount(), body.currency().toUpperCase(Locale.ROOT),
                body.reference()));

        String baseUrl = ApiPaths.baseUrl();
        return ApiResponses.created(
                assembler.toResource(movement, baseUrl),
                URI.create(ApiPaths.transaction(baseUrl, movement.transactionId())));
    }
}
