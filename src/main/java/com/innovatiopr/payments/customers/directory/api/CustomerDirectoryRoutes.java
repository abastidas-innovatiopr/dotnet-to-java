package com.innovatiopr.payments.customers.directory.api;

import com.innovatiopr.payments.shared.api.ApiPaths;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.RouterFunctions;
import org.springframework.web.servlet.function.ServerResponse;

@Configuration(proxyBeanMethods = false)
class CustomerDirectoryRoutes {

    @Bean
    RouterFunction<ServerResponse> customerDirectoryRouterFunction(CustomerDirectoryEndpoint endpoint) {
        return RouterFunctions.route()
                .GET(ApiPaths.CUSTOMER, endpoint::getById)
                .GET(ApiPaths.CUSTOMERS, endpoint::list)
                .build();
    }
}
