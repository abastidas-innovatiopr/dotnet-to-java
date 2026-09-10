package com.innovatiopr.payments.ledger.statements.api;

import com.innovatiopr.payments.shared.api.ApiPaths;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.RouterFunctions;
import org.springframework.web.servlet.function.ServerResponse;

@Configuration(proxyBeanMethods = false)
class StatementRoutes {

    @Bean
    RouterFunction<ServerResponse> statementRouterFunction(StatementEndpoint endpoint) {
        return RouterFunctions.route()
                .GET(ApiPaths.ACCOUNT_STATEMENT, endpoint::getStatement)
                .build();
    }
}
