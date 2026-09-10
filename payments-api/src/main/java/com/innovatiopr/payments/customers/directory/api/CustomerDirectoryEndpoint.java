package com.innovatiopr.payments.customers.directory.api;

import com.innovatiopr.payments.customers.CustomerId;
import com.innovatiopr.payments.customers.directory.application.CustomerSummary;
import com.innovatiopr.payments.customers.directory.application.GetCustomerHandler;
import com.innovatiopr.payments.customers.directory.application.GetCustomerQuery;
import com.innovatiopr.payments.customers.directory.application.ListCustomersHandler;
import com.innovatiopr.payments.customers.directory.application.ListCustomersQuery;
import com.innovatiopr.payments.shared.api.ApiPaths;
import com.innovatiopr.payments.shared.api.ApiResponses;
import com.innovatiopr.payments.shared.api.PageParams;
import com.innovatiopr.payments.shared.api.PagedResources;
import com.innovatiopr.payments.shared.application.PageRequest;
import com.innovatiopr.payments.shared.application.PageResult;
import com.innovatiopr.payments.shared.domain.Result;
import org.springframework.hateoas.MediaTypes;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.function.ServerRequest;
import org.springframework.web.servlet.function.ServerResponse;

import java.util.List;
import java.util.UUID;

@Component
class CustomerDirectoryEndpoint {

    private final GetCustomerHandler getCustomer;
    private final ListCustomersHandler listCustomers;
    private final CustomerResourceAssembler assembler;

    CustomerDirectoryEndpoint(GetCustomerHandler getCustomer, ListCustomersHandler listCustomers,
                              CustomerResourceAssembler assembler) {
        this.getCustomer = getCustomer;
        this.listCustomers = listCustomers;
        this.assembler = assembler;
    }

    ServerResponse getById(ServerRequest request) {
        UUID customerId;
        try {
            customerId = UUID.fromString(request.pathVariable("customerId"));
        } catch (IllegalArgumentException e) {
            return ApiResponses.invalidIdentifier("customer id", request.pathVariable("customerId"));
        }

        String baseUrl = ApiPaths.baseUrl(request);
        return ApiResponses.ok(getCustomer.handle(new GetCustomerQuery(CustomerId.of(customerId))),
                details -> assembler.toResource(details, baseUrl));
    }

    ServerResponse list(ServerRequest request) {
        PageRequest page = PageParams.from(request, "registeredAt");
        Result<PageResult<CustomerSummary>> result = listCustomers.handle(new ListCustomersQuery(page));

        String baseUrl = ApiPaths.baseUrl(request);
        return result.fold(
                found -> {
                    List<CustomerResource> resources = found.items().stream()
                            .map(summary -> assembler.toResource(summary, baseUrl))
                            .toList();
                    return ServerResponse.ok()
                            .contentType(MediaTypes.HAL_JSON)
                            .body(PagedResources.of(found, resources, request, ApiPaths.customers(baseUrl)));
                },
                ApiResponses::problem);
    }
}
