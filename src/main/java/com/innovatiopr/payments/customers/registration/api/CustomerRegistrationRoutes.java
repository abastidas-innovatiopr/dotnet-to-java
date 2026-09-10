package com.innovatiopr.payments.customers.registration.api;

import com.innovatiopr.payments.shared.api.ApiPaths;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.RouterFunctions;
import org.springframework.web.servlet.function.ServerResponse;

/**
 * Functional routes for customer registration.
 *
 * <p>The Spring equivalent of {@code app.MapPost("/api/v1/customers", CreateCustomer)} in ASP.NET Core
 * Minimal APIs. Routes are declared as a {@code @Bean} of type {@code RouterFunction}; Spring composes
 * every such bean into the one the {@code DispatcherServlet} consults.
 *
 * <p>Grouped by feature, not by resource, so a slice's routes sit next to the handler and the command they
 * invoke.
 */
@Configuration(proxyBeanMethods = false)
class CustomerRegistrationRoutes {

    @Bean
    RouterFunction<ServerResponse> customerRegistrationRouterFunction(CustomerRegistrationEndpoint endpoint) {
        return RouterFunctions.route()
                .POST(ApiPaths.CUSTOMERS, endpoint::create)
                .build();
    }
}
