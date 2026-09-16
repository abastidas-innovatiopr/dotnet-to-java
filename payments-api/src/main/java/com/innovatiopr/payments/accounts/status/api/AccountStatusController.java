package com.innovatiopr.payments.accounts.status.api;

import com.innovatiopr.payments.accounts.AccountId;
import com.innovatiopr.payments.accounts.details.api.AccountResource;
import com.innovatiopr.payments.accounts.details.api.AccountResourceAssembler;
import com.innovatiopr.payments.accounts.details.application.GetAccountHandler;
import com.innovatiopr.payments.accounts.details.application.GetAccountQuery;
import com.innovatiopr.payments.accounts.status.application.AccountStatusCommands;
import com.innovatiopr.payments.accounts.status.application.CloseAccountHandler;
import com.innovatiopr.payments.accounts.status.application.FreezeAccountHandler;
import com.innovatiopr.payments.accounts.status.application.UnfreezeAccountHandler;
import com.innovatiopr.payments.shared.api.ApiPaths;
import com.innovatiopr.payments.shared.api.docs.OpenApiDocs;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.hateoas.MediaTypes;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * Administrative lifecycle endpoints.
 *
 * <p>Each returns the full account representation, so the response already advertises the affordances the
 * new state allows: freeze an account and the {@code withdraw} link is gone from the reply, replaced by
 * {@code unfreeze}. A client never has to guess what changed.
 */
@RestController
@RequestMapping(path = ApiPaths.ACCOUNTS, produces = MediaTypes.HAL_JSON_VALUE)
@Tag(name = OpenApiDocs.TAG_ACCOUNTS)
public class AccountStatusController {

    private final FreezeAccountHandler freeze;
    private final UnfreezeAccountHandler unfreeze;
    private final CloseAccountHandler close;
    private final GetAccountHandler getAccount;
    private final AccountResourceAssembler assembler;

    AccountStatusController(FreezeAccountHandler freeze, UnfreezeAccountHandler unfreeze,
                            CloseAccountHandler close, GetAccountHandler getAccount,
                            AccountResourceAssembler assembler) {
        this.freeze = freeze;
        this.unfreeze = unfreeze;
        this.close = close;
        this.getAccount = getAccount;
        this.assembler = assembler;
    }

    @PostMapping("/{accountId}/freeze")
    @Operation(
            operationId = "freezeAccount",
            summary = "Freeze an account",
            description = """
                    A frozen account can neither send nor receive money. The response is the full \
                    account representation, so it already shows the reduced set of affordances — \
                    `deposit`, `withdraw` and `transfer` are gone, `unfreeze` has appeared.

                    Freezing an already-frozen account succeeds and changes nothing.""")
    @ApiResponse(responseCode = "200", description = "The account, now FROZEN.")
    @ApiResponse(responseCode = "404", description = "`ACCOUNT_NOT_FOUND`.")
    @ApiResponse(responseCode = "422",
            description = "`ACCOUNT_INVALID_STATUS_TRANSITION` — a closed account cannot be frozen.")
    public AccountResource freeze(
            @PathVariable @Parameter(description = OpenApiDocs.ACCOUNT_ID_DESCRIPTION) UUID accountId) {
        freeze.handle(new AccountStatusCommands.FreezeAccountCommand(AccountId.of(accountId)));
        return currentState(accountId);
    }

    @PostMapping("/{accountId}/unfreeze")
    @Operation(operationId = "unfreezeAccount", summary = "Unfreeze an account")
    @ApiResponse(responseCode = "200",
            description = "The account, ACTIVE again, with its action links restored.")
    @ApiResponse(responseCode = "404", description = "`ACCOUNT_NOT_FOUND`.")
    @ApiResponse(responseCode = "422", description = "`ACCOUNT_INVALID_STATUS_TRANSITION`.")
    public AccountResource unfreeze(
            @PathVariable @Parameter(description = OpenApiDocs.ACCOUNT_ID_DESCRIPTION) UUID accountId) {
        unfreeze.handle(new AccountStatusCommands.UnfreezeAccountCommand(AccountId.of(accountId)));
        return currentState(accountId);
    }

    @PostMapping("/{accountId}/close")
    @Operation(
            operationId = "closeAccount",
            summary = "Close an account",
            description = "Terminal. Only permitted at a zero balance — empty the account first.")
    @ApiResponse(responseCode = "200", description = "The account, now CLOSED, advertising no actions.")
    @ApiResponse(responseCode = "404", description = "`ACCOUNT_NOT_FOUND`.")
    @ApiResponse(responseCode = "422", description = "`ACCOUNT_NOT_EMPTY` — the account still holds money.")
    public AccountResource close(
            @PathVariable @Parameter(description = OpenApiDocs.ACCOUNT_ID_DESCRIPTION) UUID accountId) {
        close.handle(new AccountStatusCommands.CloseAccountCommand(AccountId.of(accountId)));
        return currentState(accountId);
    }

    /** Re-reads through the query side so the response is the same representation {@code GET} would give. */
    private AccountResource currentState(UUID accountId) {
        return assembler.toResource(
                getAccount.handle(new GetAccountQuery(AccountId.of(accountId))),
                ApiPaths.baseUrl());
    }
}
