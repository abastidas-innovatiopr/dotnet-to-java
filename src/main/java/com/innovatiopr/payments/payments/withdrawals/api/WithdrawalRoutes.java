package com.innovatiopr.payments.payments.withdrawals.api;

import com.innovatiopr.payments.shared.api.ApiPaths;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.RouterFunctions;
import org.springframework.web.servlet.function.ServerResponse;

@Configuration(proxyBeanMethods = false)
class WithdrawalRoutes {

    @Bean
    RouterFunction<ServerResponse> withdrawalRouterFunction(WithdrawalEndpoint endpoint) {
        return RouterFunctions.route()
                .POST(ApiPaths.ACCOUNT_WITHDRAWALS, endpoint::withdraw)
                .build();
    }
}
