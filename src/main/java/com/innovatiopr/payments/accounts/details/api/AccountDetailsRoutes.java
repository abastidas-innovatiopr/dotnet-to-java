package com.innovatiopr.payments.accounts.details.api;

import com.innovatiopr.payments.shared.api.ApiPaths;
import com.innovatiopr.payments.shared.api.docs.OpenApiDocs;
import org.springdoc.core.fn.builders.operation.Builder;
import org.springdoc.webmvc.core.fn.SpringdocRouteBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.ServerResponse;

@Configuration(proxyBeanMethods = false)
class AccountDetailsRoutes {

    @Bean
    RouterFunction<ServerResponse> accountDetailsRouterFunction(AccountDetailsEndpoint endpoint) {
        return SpringdocRouteBuilder.route()
                .GET(ApiPaths.ACCOUNT_BALANCE, endpoint::getBalance, AccountDetailsRoutes::balanceDocs)
                .GET(ApiPaths.ACCOUNT, endpoint::getById, AccountDetailsRoutes::accountDocs)
                .build();
    }

    private static void accountDocs(Builder operation) {
        operation
                .operationId("getAccount")
                .tags(new String[]{OpenApiDocs.TAG_ACCOUNTS})
                .summary("Fetch one account")
                .description("""
                        **The links depend on the account's state.** A client should not have to encode the \
                        bank's rules; it looks for a link and finds one, or does not.

                        | State | Action links |
                        |---|---|
                        | `ACTIVE` | `deposit`, `withdraw`, `transfer`, `freeze` (plus `close` only at a zero balance) |
                        | `FROZEN` | `unfreeze` only — a frozen account can neither send nor receive |
                        | `CLOSED` | none; read links only |

                        Read links (`self`, `balance`, `transactions`, `statement`, `customer`) are always \
                        present, because a closed account remains auditable.""")
                .parameter(OpenApiDocs.uuidPathParam("accountId", "Account identifier."))
                .response(OpenApiDocs.hal("200", "The account, with state-appropriate links."))
                .response(OpenApiDocs.problem("400", "The path variable is not a UUID."))
                .response(OpenApiDocs.problem("404", "`ACCOUNT_NOT_FOUND`."));
    }

    private static void balanceDocs(Builder operation) {
        operation
                .operationId("getAccountBalance")
                .tags(new String[]{OpenApiDocs.TAG_ACCOUNTS})
                .summary("Fetch an account balance")
                .description("The current-state projection. The auditable history is the account statement.")
                .parameter(OpenApiDocs.uuidPathParam("accountId", "Account identifier."))
                .response(OpenApiDocs.hal("200", "Currency, balance, status and the time observed."))
                .response(OpenApiDocs.problem("404", "`ACCOUNT_NOT_FOUND`."));
    }
}
