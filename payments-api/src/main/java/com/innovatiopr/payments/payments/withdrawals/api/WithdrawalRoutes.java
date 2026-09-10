package com.innovatiopr.payments.payments.withdrawals.api;

import com.innovatiopr.payments.payments.deposits.api.CashMovementRequest;
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
class WithdrawalRoutes {

    @Bean
    RouterFunction<ServerResponse> withdrawalRouterFunction(WithdrawalEndpoint endpoint) {
        return SpringdocRouteBuilder.route()
                .POST(ApiPaths.ACCOUNT_WITHDRAWALS, endpoint::withdraw, WithdrawalRoutes::withdrawDocs)
                .build();
    }

    private static void withdrawDocs(Builder operation) {
        operation
                .operationId("withdrawMoney")
                .tags(new String[]{OpenApiDocs.TAG_PAYMENTS})
                .summary("Withdraw money from an account")
                .description("""
                        Cash out of the system. Debits the account and credits the settlement position.

                        The account row is locked for the duration, so concurrent withdrawals cannot both \
                        pass the sufficient-funds check.""")
                .parameter(OpenApiDocs.uuidPathParam("accountId", "Account to debit."))
                .requestBody(requestBodyBuilder().required(true).implementation(CashMovementRequest.class))
                .response(OpenApiDocs.hal("201", "Withdrawal recorded. Carries the resulting balance."))
                .response(OpenApiDocs.problem("400", "Invalid body or an unrecognised currency."))
                .response(OpenApiDocs.problem("404", "`ACCOUNT_NOT_FOUND`."))
                .response(OpenApiDocs.problem("422",
                        "`ACCOUNT_INSUFFICIENT_FUNDS`, `ACCOUNT_FROZEN`, `ACCOUNT_CLOSED` or "
                                + "`MONEY_CURRENCY_MISMATCH`."));
    }
}
