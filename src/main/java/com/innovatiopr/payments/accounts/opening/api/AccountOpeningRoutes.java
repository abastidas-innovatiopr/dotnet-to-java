package com.innovatiopr.payments.accounts.opening.api;

import com.innovatiopr.payments.shared.api.ApiPaths;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.RouterFunctions;
import org.springframework.web.servlet.function.ServerResponse;

@Configuration(proxyBeanMethods = false)
class AccountOpeningRoutes {

    @Bean
    RouterFunction<ServerResponse> accountOpeningRouterFunction(AccountOpeningEndpoint endpoint) {
        return RouterFunctions.route()
                .POST(ApiPaths.ACCOUNTS, endpoint::open)
                .build();
    }
}
