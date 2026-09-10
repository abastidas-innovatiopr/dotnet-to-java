package com.innovatiopr.payments.customers.directory.api;

import com.innovatiopr.payments.shared.api.ApiPaths;
import com.innovatiopr.payments.shared.api.docs.OpenApiDocs;
import org.springdoc.core.fn.builders.operation.Builder;
import org.springdoc.webmvc.core.fn.SpringdocRouteBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.ServerResponse;

@Configuration(proxyBeanMethods = false)
class CustomerDirectoryRoutes {

    @Bean
    RouterFunction<ServerResponse> customerDirectoryRouterFunction(CustomerDirectoryEndpoint endpoint) {
        return SpringdocRouteBuilder.route()
                .GET(ApiPaths.CUSTOMER, endpoint::getById, CustomerDirectoryRoutes::getDocs)
                .GET(ApiPaths.CUSTOMERS, endpoint::list, CustomerDirectoryRoutes::listDocs)
                .build();
    }

    private static void getDocs(Builder operation) {
        OpenApiDocs.withNotFound(operation
                .operationId("getCustomer")
                .tags(new String[]{OpenApiDocs.TAG_CUSTOMERS})
                .summary("Fetch one customer")
                .parameter(OpenApiDocs.uuidPathParam("customerId", "Customer identifier."))
                .response(OpenApiDocs.hal("200", "The customer, with links to itself and its accounts.")),
                "The customer");
    }

    private static void listDocs(Builder operation) {
        OpenApiDocs.withPagination(operation
                .operationId("listCustomers")
                .tags(new String[]{OpenApiDocs.TAG_CUSTOMERS})
                .summary("List customers")
                .description("""
                        Paginated. Each row carries `accountCount`, which is computed by the read model — \
                        the Customer aggregate has no such field, because accounts are opened by a \
                        different module and it could not keep the number correct.""")
                .response(OpenApiDocs.hal("200",
                        "A page of customers with `page` metadata and navigation links.")),
                "`registeredAt`, `lastName`, `email`", "registeredAt");
    }
}
