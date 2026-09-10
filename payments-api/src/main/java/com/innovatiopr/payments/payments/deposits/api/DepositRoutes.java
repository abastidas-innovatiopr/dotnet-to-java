package com.innovatiopr.payments.payments.deposits.api;

import com.innovatiopr.payments.shared.api.ApiPaths;
import com.innovatiopr.payments.shared.api.docs.OpenApiDocs;
import org.springdoc.core.fn.builders.operation.Builder;
import org.springdoc.webmvc.core.fn.SpringdocRouteBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.ServerResponse;

import static org.springdoc.core.fn.builders.requestbody.Builder.requestBodyBuilder;

/**
 * Routes for deposits.
 *
 * <p>The path sits under {@code /accounts/{id}} because that is the right resource hierarchy for a
 * client, while the code lives in the Payments module because that is the right ownership for a financial
 * operation. URL shape and module ownership are separate decisions.
 */
@Configuration(proxyBeanMethods = false)
class DepositRoutes {

    @Bean
    RouterFunction<ServerResponse> depositRouterFunction(DepositEndpoint endpoint) {
        return SpringdocRouteBuilder.route()
                .POST(ApiPaths.ACCOUNT_DEPOSITS, endpoint::deposit, DepositRoutes::depositDocs)
                .build();
    }

    private static void depositDocs(Builder operation) {
        operation
                .operationId("depositMoney")
                .tags(new String[]{OpenApiDocs.TAG_PAYMENTS})
                .summary("Deposit money into an account")
                .description("""
                        Cash in from outside the system. Credits the account and debits the settlement \
                        position, so the ledger stays balanced — double entry only works if every posting \
                        has a counterparty.

                        This is also how a newly opened account gets its opening balance.""")
                .parameter(OpenApiDocs.uuidPathParam("accountId", "Account to credit."))
                .requestBody(requestBodyBuilder().required(true).implementation(CashMovementRequest.class))
                .response(OpenApiDocs.hal("201", "Deposit recorded. Carries the resulting balance."))
                .response(OpenApiDocs.problem("400", "Invalid body or an unrecognised currency."))
                .response(OpenApiDocs.problem("404", "`ACCOUNT_NOT_FOUND`."))
                .response(OpenApiDocs.problem("422",
                        "`ACCOUNT_FROZEN`, `ACCOUNT_CLOSED` or `MONEY_CURRENCY_MISMATCH`."));
    }
}
