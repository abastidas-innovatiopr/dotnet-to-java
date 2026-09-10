package com.innovatiopr.payments.accounts.opening.api;

import com.innovatiopr.payments.shared.api.ApiPaths;
import com.innovatiopr.payments.shared.api.docs.OpenApiDocs;
import org.springdoc.core.fn.builders.operation.Builder;
import org.springdoc.webmvc.core.fn.SpringdocRouteBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.ServerResponse;

import static org.springdoc.core.fn.builders.requestbody.Builder.requestBodyBuilder;

@Configuration(proxyBeanMethods = false)
class AccountOpeningRoutes {

    @Bean
    RouterFunction<ServerResponse> accountOpeningRouterFunction(AccountOpeningEndpoint endpoint) {
        return SpringdocRouteBuilder.route()
                .POST(ApiPaths.ACCOUNTS, endpoint::open, AccountOpeningRoutes::openDocs)
                .build();
    }

    private static void openDocs(Builder operation) {
        operation
                .operationId("openAccount")
                .tags(new String[]{OpenApiDocs.TAG_ACCOUNTS})
                .summary("Open an account")
                .description("""
                        Opens a new account with a **zero balance**. There is deliberately no opening \
                        balance parameter: money that appeared without a ledger posting could not be \
                        reconciled, so funding is a separate deposit which writes balanced postings.

                        Fund the account with `POST /api/v1/accounts/{accountId}/deposits`.""")
                .requestBody(requestBodyBuilder().required(true).implementation(OpenAccountRequest.class))
                .response(OpenApiDocs.hal("201", "Account opened, empty and ACTIVE."))
                .response(OpenApiDocs.problem("400", "Invalid body, or an unrecognised currency code."))
                .response(OpenApiDocs.problem("404", "`CUSTOMER_NOT_FOUND`."));
    }
}
