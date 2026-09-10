package com.innovatiopr.payments.customers.registration.api;

import com.innovatiopr.payments.shared.api.ApiPaths;
import com.innovatiopr.payments.shared.api.docs.OpenApiDocs;
import org.springdoc.core.fn.builders.operation.Builder;
import org.springdoc.webmvc.core.fn.SpringdocRouteBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.ServerResponse;

import static org.springdoc.core.fn.builders.requestbody.Builder.requestBodyBuilder;

/**
 * Functional routes for customer registration.
 *
 * <p>The Spring equivalent of {@code app.MapPost("/api/v1/customers", CreateCustomer)} in ASP.NET Core
 * Minimal APIs. Routes are declared as a {@code @Bean} of type {@code RouterFunction}; Spring composes
 * every such bean into the one the {@code DispatcherServlet} consults.
 *
 * <p>Grouped by feature, not by resource, so a slice's routes sit next to the handler and the command
 * they invoke.
 */
@Configuration(proxyBeanMethods = false)
class CustomerRegistrationRoutes {

    @Bean
    RouterFunction<ServerResponse> customerRegistrationRouterFunction(CustomerRegistrationEndpoint endpoint) {
        return SpringdocRouteBuilder.route()
                .POST(ApiPaths.CUSTOMERS, endpoint::create, CustomerRegistrationRoutes::createDocs)
                .build();
    }

    private static void createDocs(Builder operation) {
        operation
                .operationId("createCustomer")
                .tags(new String[]{OpenApiDocs.TAG_CUSTOMERS})
                .summary("Register a customer")
                .description("""
                        Registers a customer. The email must be unique across all customers — a rule about \
                        the set rather than about one customer, which is why it lives in the handler and \
                        not in the aggregate.""")
                .requestBody(requestBodyBuilder().required(true).implementation(CreateCustomerRequest.class))
                .response(OpenApiDocs.hal("201", "Customer registered. `Location` points at the new resource."))
                .response(OpenApiDocs.problem("400", "Invalid body — see the `errors` array."))
                .response(OpenApiDocs.problem("409", "`CUSTOMER_EMAIL_ALREADY_REGISTERED`."));
    }
}
