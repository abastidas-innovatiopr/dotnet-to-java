package com.innovatiopr.payments.accounts.status.api;

import com.innovatiopr.payments.accounts.AccountId;
import com.innovatiopr.payments.accounts.details.api.AccountResourceAssembler;
import com.innovatiopr.payments.accounts.details.application.GetAccountHandler;
import com.innovatiopr.payments.accounts.details.application.GetAccountQuery;
import com.innovatiopr.payments.accounts.status.application.AccountStatusCommands;
import com.innovatiopr.payments.accounts.status.application.AccountStatusResult;
import com.innovatiopr.payments.accounts.status.application.CloseAccountHandler;
import com.innovatiopr.payments.accounts.status.application.FreezeAccountHandler;
import com.innovatiopr.payments.accounts.status.application.UnfreezeAccountHandler;
import com.innovatiopr.payments.shared.api.ApiPaths;
import com.innovatiopr.payments.shared.api.ApiResponses;
import com.innovatiopr.payments.shared.domain.Result;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.function.ServerRequest;
import org.springframework.web.servlet.function.ServerResponse;

import java.util.UUID;
import java.util.function.Function;

/**
 * Administrative lifecycle endpoints.
 *
 * <p>Each returns the full account representation, so the response already advertises the affordances the
 * new state allows: freeze an account and the {@code withdraw} link is gone from the reply, replaced by
 * {@code unfreeze}. A client never has to guess what changed.
 */
@Component
class AccountStatusEndpoint {

    private final FreezeAccountHandler freeze;
    private final UnfreezeAccountHandler unfreeze;
    private final CloseAccountHandler close;
    private final GetAccountHandler getAccount;
    private final AccountResourceAssembler assembler;

    AccountStatusEndpoint(FreezeAccountHandler freeze, UnfreezeAccountHandler unfreeze,
                          CloseAccountHandler close, GetAccountHandler getAccount,
                          AccountResourceAssembler assembler) {
        this.freeze = freeze;
        this.unfreeze = unfreeze;
        this.close = close;
        this.getAccount = getAccount;
        this.assembler = assembler;
    }

    ServerResponse freeze(ServerRequest request) {
        return apply(request, id -> freeze.handle(new AccountStatusCommands.FreezeAccountCommand(id)));
    }

    ServerResponse unfreeze(ServerRequest request) {
        return apply(request, id -> unfreeze.handle(new AccountStatusCommands.UnfreezeAccountCommand(id)));
    }

    ServerResponse close(ServerRequest request) {
        return apply(request, id -> close.handle(new AccountStatusCommands.CloseAccountCommand(id)));
    }

    private ServerResponse apply(ServerRequest request,
                                 Function<AccountId, Result<AccountStatusResult>> transition) {
        UUID accountId;
        try {
            accountId = UUID.fromString(request.pathVariable("accountId"));
        } catch (IllegalArgumentException e) {
            return ApiResponses.invalidIdentifier("account id", request.pathVariable("accountId"));
        }

        Result<AccountStatusResult> result = transition.apply(AccountId.of(accountId));
        if (result.isFailure()) {
            return ApiResponses.problem(result.errors());
        }

        String baseUrl = ApiPaths.baseUrl(request);
        return ApiResponses.ok(getAccount.handle(new GetAccountQuery(AccountId.of(accountId))),
                details -> assembler.toResource(details, baseUrl));
    }
}
