package com.innovatiopr.payments.accounts.details.api;

import com.innovatiopr.payments.shared.api.ApiPaths;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.RouterFunctions;
import org.springframework.web.servlet.function.ServerResponse;

@Configuration(proxyBeanMethods = false)
class AccountDetailsRoutes {

    @Bean
    RouterFunction<ServerResponse> accountDetailsRouterFunction(AccountDetailsEndpoint endpoint) {
        return RouterFunctions.route()
                .GET(ApiPaths.ACCOUNT_BALANCE, endpoint::getBalance)
                .GET(ApiPaths.ACCOUNT, endpoint::getById)
                .build();
    }
}
