package com.innovatiopr.payments.accounts.status.api;

import com.innovatiopr.payments.shared.api.ApiPaths;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.RouterFunctions;
import org.springframework.web.servlet.function.ServerResponse;

@Configuration(proxyBeanMethods = false)
class AccountStatusRoutes {

    @Bean
    RouterFunction<ServerResponse> accountStatusRouterFunction(AccountStatusEndpoint endpoint) {
        return RouterFunctions.route()
                .POST(ApiPaths.ACCOUNT_FREEZE, endpoint::freeze)
                .POST(ApiPaths.ACCOUNT_UNFREEZE, endpoint::unfreeze)
                .POST(ApiPaths.ACCOUNT_CLOSE, endpoint::close)
                .build();
    }
}
