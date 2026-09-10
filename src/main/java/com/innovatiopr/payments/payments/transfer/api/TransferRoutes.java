package com.innovatiopr.payments.payments.transfer.api;

import com.innovatiopr.payments.shared.api.ApiPaths;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.RouterFunctions;
import org.springframework.web.servlet.function.ServerResponse;

/**
 * Routes for the transfer feature.
 *
 * <p>Compare with ASP.NET Core Minimal APIs:
 * <pre>{@code
 * // .NET
 * app.MapPost("/api/v1/transfers", TransferMoney);
 *
 * // Spring
 * RouterFunctions.route().POST("/api/v1/transfers", endpoint::transfer).build();
 * }</pre>
 *
 * <p>{@code @Transactional} must never appear on a {@code RouterFunction} bean or a handler function. The
 * bean is built once at startup, and the handler is a method reference invoked by the
 * {@code DispatcherServlet} rather than through a Spring proxy — so the annotation would be silently
 * inert. Transactions belong on the application handlers.
 */
@Configuration(proxyBeanMethods = false)
class TransferRoutes {

    @Bean
    RouterFunction<ServerResponse> transferRouterFunction(TransferEndpoint endpoint) {
        return RouterFunctions.route()
                .POST(ApiPaths.TRANSFERS, endpoint::transfer)
                .build();
    }
}
