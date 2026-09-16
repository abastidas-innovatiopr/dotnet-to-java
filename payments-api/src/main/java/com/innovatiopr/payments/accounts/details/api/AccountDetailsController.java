package com.innovatiopr.payments.accounts.details.api;

import com.innovatiopr.payments.accounts.AccountId;
import com.innovatiopr.payments.accounts.details.application.GetAccountHandler;
import com.innovatiopr.payments.accounts.details.application.GetAccountQuery;
import com.innovatiopr.payments.accounts.details.application.GetBalanceHandler;
import com.innovatiopr.payments.accounts.details.application.GetBalanceQuery;
import com.innovatiopr.payments.shared.api.ApiPaths;
import com.innovatiopr.payments.shared.api.docs.OpenApiDocs;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.hateoas.MediaTypes;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping(path = ApiPaths.ACCOUNTS, produces = MediaTypes.HAL_JSON_VALUE)
@Tag(name = OpenApiDocs.TAG_ACCOUNTS)
public class AccountDetailsController {

    private final GetAccountHandler getAccount;
    private final GetBalanceHandler getBalance;
    private final AccountResourceAssembler assembler;

    AccountDetailsController(GetAccountHandler getAccount, GetBalanceHandler getBalance,
                             AccountResourceAssembler assembler) {
        this.getAccount = getAccount;
        this.getBalance = getBalance;
        this.assembler = assembler;
    }

    @GetMapping("/{accountId}")
    @Operation(
            operationId = "getAccount",
            summary = "Fetch one account",
            description = """
                    **The links depend on the account's state.** A client should not have to encode the \
                    bank's rules; it looks for a link and finds one, or does not.

                    | State | Action links |
                    |---|---|
                    | `ACTIVE` | `deposit`, `withdraw`, `transfer`, `freeze` (plus `close` only at a zero balance) |
                    | `FROZEN` | `unfreeze` only — a frozen account can neither send nor receive |
                    | `CLOSED` | none; read links only |

                    Read links (`self`, `balance`, `transactions`, `statement`, `customer`) are always \
                    present, because a closed account remains auditable.""")
    @ApiResponse(responseCode = "200", description = "The account, with state-appropriate links.")
    @ApiResponse(responseCode = "400", description = "The path variable is not a UUID.")
    @ApiResponse(responseCode = "404", description = "`ACCOUNT_NOT_FOUND`.")
    public AccountResource getById(
            @PathVariable @Parameter(description = OpenApiDocs.ACCOUNT_ID_DESCRIPTION) UUID accountId) {
        return assembler.toResource(
                getAccount.handle(new GetAccountQuery(AccountId.of(accountId))),
                ApiPaths.baseUrl());
    }

    @GetMapping("/{accountId}/balance")
    @Operation(
            operationId = "getAccountBalance",
            summary = "Fetch an account balance",
            description = "The current-state projection. The auditable history is the account statement.")
    @ApiResponse(responseCode = "200", description = "Currency, balance, status and the time observed.")
    @ApiResponse(responseCode = "404", description = "`ACCOUNT_NOT_FOUND`.")
    public BalanceResource getBalance(
            @PathVariable @Parameter(description = OpenApiDocs.ACCOUNT_ID_DESCRIPTION) UUID accountId) {
        return assembler.toBalanceResource(
                getBalance.handle(new GetBalanceQuery(AccountId.of(accountId))),
                ApiPaths.baseUrl());
    }
}
