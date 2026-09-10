package com.innovatiopr.payments.accounts.status.api;

import com.innovatiopr.payments.shared.api.ApiPaths;
import com.innovatiopr.payments.shared.api.docs.OpenApiDocs;
import org.springdoc.core.fn.builders.operation.Builder;
import org.springdoc.webmvc.core.fn.SpringdocRouteBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.ServerResponse;

@Configuration(proxyBeanMethods = false)
class AccountStatusRoutes {

    @Bean
    RouterFunction<ServerResponse> accountStatusRouterFunction(AccountStatusEndpoint endpoint) {
        return SpringdocRouteBuilder.route()
                .POST(ApiPaths.ACCOUNT_FREEZE, endpoint::freeze, AccountStatusRoutes::freezeDocs)
                .POST(ApiPaths.ACCOUNT_UNFREEZE, endpoint::unfreeze, AccountStatusRoutes::unfreezeDocs)
                .POST(ApiPaths.ACCOUNT_CLOSE, endpoint::close, AccountStatusRoutes::closeDocs)
                .build();
    }

    private static void freezeDocs(Builder operation) {
        operation
                .operationId("freezeAccount")
                .tags(new String[]{OpenApiDocs.TAG_ACCOUNTS})
                .summary("Freeze an account")
                .description("""
                        A frozen account can neither send nor receive money. The response is the full \
                        account representation, so it already shows the reduced set of affordances — \
                        `deposit`, `withdraw` and `transfer` are gone, `unfreeze` has appeared.

                        Freezing an already-frozen account succeeds and changes nothing.""")
                .parameter(OpenApiDocs.uuidPathParam("accountId", "Account identifier."))
                .response(OpenApiDocs.hal("200", "The account, now FROZEN."))
                .response(OpenApiDocs.problem("404", "`ACCOUNT_NOT_FOUND`."))
                .response(OpenApiDocs.problem("422",
                        "`ACCOUNT_INVALID_STATUS_TRANSITION` — a closed account cannot be frozen."));
    }

    private static void unfreezeDocs(Builder operation) {
        operation
                .operationId("unfreezeAccount")
                .tags(new String[]{OpenApiDocs.TAG_ACCOUNTS})
                .summary("Unfreeze an account")
                .parameter(OpenApiDocs.uuidPathParam("accountId", "Account identifier."))
                .response(OpenApiDocs.hal("200", "The account, ACTIVE again, with its action links restored."))
                .response(OpenApiDocs.problem("404", "`ACCOUNT_NOT_FOUND`."))
                .response(OpenApiDocs.problem("422", "`ACCOUNT_INVALID_STATUS_TRANSITION`."));
    }

    private static void closeDocs(Builder operation) {
        operation
                .operationId("closeAccount")
                .tags(new String[]{OpenApiDocs.TAG_ACCOUNTS})
                .summary("Close an account")
                .description("Terminal. Only permitted at a zero balance — empty the account first.")
                .parameter(OpenApiDocs.uuidPathParam("accountId", "Account identifier."))
                .response(OpenApiDocs.hal("200", "The account, now CLOSED, advertising no actions."))
                .response(OpenApiDocs.problem("404", "`ACCOUNT_NOT_FOUND`."))
                .response(OpenApiDocs.problem("422",
                        "`ACCOUNT_NOT_EMPTY` — the account still holds money."));
    }
}
