package com.innovatiopr.payments.accounts.opening.api;

import com.innovatiopr.payments.accounts.details.api.AccountResource;
import com.innovatiopr.payments.accounts.details.api.AccountResourceAssembler;
import com.innovatiopr.payments.accounts.details.application.AccountDetails;
import com.innovatiopr.payments.accounts.opening.application.OpenAccountCommand;
import com.innovatiopr.payments.accounts.opening.application.OpenAccountHandler;
import com.innovatiopr.payments.accounts.opening.application.OpenAccountResult;
import com.innovatiopr.payments.customers.CustomerId;
import com.innovatiopr.payments.shared.api.ApiPaths;
import com.innovatiopr.payments.shared.api.ApiResponses;
import com.innovatiopr.payments.shared.api.docs.OpenApiDocs;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.hateoas.MediaTypes;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.Locale;

@RestController
@RequestMapping(path = ApiPaths.ACCOUNTS, produces = MediaTypes.HAL_JSON_VALUE)
@Tag(name = OpenApiDocs.TAG_ACCOUNTS)
public class AccountOpeningController {

    private final OpenAccountHandler openAccount;
    private final AccountResourceAssembler assembler;

    AccountOpeningController(OpenAccountHandler openAccount, AccountResourceAssembler assembler) {
        this.openAccount = openAccount;
        this.assembler = assembler;
    }

    @PostMapping
    @Operation(
            operationId = "openAccount",
            summary = "Open an account",
            description = """
                    Opens a new account with a **zero balance**. There is deliberately no opening \
                    balance parameter: money that appeared without a ledger posting could not be \
                    reconciled, so funding is a separate deposit which writes balanced postings.

                    Fund the account with `POST /api/v1/accounts/{accountId}/deposits`.""")
    @ApiResponse(responseCode = "201", description = "Account opened, empty and ACTIVE.")
    @ApiResponse(responseCode = "400", description = "Invalid body, or an unrecognised currency code.")
    @ApiResponse(responseCode = "404", description = "`CUSTOMER_NOT_FOUND`.")
    public ResponseEntity<AccountResource> open(@Valid @RequestBody OpenAccountRequest body) {
        OpenAccountResult opened = openAccount.handle(new OpenAccountCommand(
                CustomerId.of(body.customerId()),
                body.currency().toUpperCase(Locale.ROOT)));

        String baseUrl = ApiPaths.baseUrl();
        return ApiResponses.created(
                assembler.toResource(toDetails(opened), baseUrl),
                URI.create(ApiPaths.account(baseUrl, opened.accountId())));
    }

    private static AccountDetails toDetails(OpenAccountResult result) {
        return new AccountDetails(result.accountId(), result.customerId(), result.accountNumber(),
                result.currency(), result.balance(), result.status(), result.openedAt());
    }
}
