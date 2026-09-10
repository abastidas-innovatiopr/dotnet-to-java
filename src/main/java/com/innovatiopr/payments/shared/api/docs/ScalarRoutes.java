package com.innovatiopr.payments.shared.api.docs;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.RouterFunctions;
import org.springframework.web.servlet.function.ServerResponse;

@Configuration(proxyBeanMethods = false)
class ScalarRoutes {

    @Bean
    RouterFunction<ServerResponse> scalarRouterFunction(ScalarEndpoint endpoint) {
        return RouterFunctions.route()
                .GET("/docs/scalar.js", endpoint::bundle)
                .GET("/docs", endpoint::index)
                .build();
    }
}
