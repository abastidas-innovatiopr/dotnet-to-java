package com.innovatiopr.payments.payments.transactions.api;

import com.innovatiopr.payments.shared.api.ApiPaths;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.RouterFunctions;
import org.springframework.web.servlet.function.ServerResponse;

@Configuration(proxyBeanMethods = false)
class TransactionRoutes {

    @Bean
    RouterFunction<ServerResponse> transactionRouterFunction(TransactionEndpoint endpoint) {
        return RouterFunctions.route()
                .GET(ApiPaths.ACCOUNT_TRANSACTIONS, endpoint::listForAccount)
                .GET(ApiPaths.TRANSACTION, endpoint::getById)
                .GET(ApiPaths.TRANSACTIONS, endpoint::list)
                .build();
    }
}
