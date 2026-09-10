package com.innovatiopr.payments.payments.deposits.api;

import com.innovatiopr.payments.shared.api.ApiPaths;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.RouterFunctions;
import org.springframework.web.servlet.function.ServerResponse;

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
        return RouterFunctions.route()
                .POST(ApiPaths.ACCOUNT_DEPOSITS, endpoint::deposit)
                .build();
    }
}
