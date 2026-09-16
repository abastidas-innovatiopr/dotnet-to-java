package com.innovatiopr.payments.customers.directory.api;

import com.innovatiopr.payments.customers.CustomerId;
import com.innovatiopr.payments.customers.directory.application.GetCustomerHandler;
import com.innovatiopr.payments.customers.directory.application.GetCustomerQuery;
import com.innovatiopr.payments.customers.directory.application.ListCustomersHandler;
import com.innovatiopr.payments.customers.directory.application.ListCustomersQuery;
import com.innovatiopr.payments.shared.api.ApiPaths;
import com.innovatiopr.payments.shared.api.PageQuery;
import com.innovatiopr.payments.shared.api.PagedResources;
import com.innovatiopr.payments.shared.api.docs.OpenApiDocs;
import com.innovatiopr.payments.shared.application.PageResult;
import com.innovatiopr.payments.customers.directory.application.CustomerSummary;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.hateoas.MediaTypes;
import org.springframework.hateoas.PagedModel;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping(path = ApiPaths.CUSTOMERS, produces = MediaTypes.HAL_JSON_VALUE)
@Tag(name = OpenApiDocs.TAG_CUSTOMERS)
public class CustomerDirectoryController {

    private static final String DEFAULT_SORT = "registeredAt";

    private final GetCustomerHandler getCustomer;
    private final ListCustomersHandler listCustomers;
    private final CustomerResourceAssembler assembler;

    CustomerDirectoryController(GetCustomerHandler getCustomer, ListCustomersHandler listCustomers,
                                CustomerResourceAssembler assembler) {
        this.getCustomer = getCustomer;
        this.listCustomers = listCustomers;
        this.assembler = assembler;
    }

    @GetMapping("/{customerId}")
    @Operation(operationId = "getCustomer", summary = "Fetch one customer")
    @ApiResponse(responseCode = "200", description = "The customer, with links to itself and its accounts.")
    @ApiResponse(responseCode = "404", description = "The customer does not exist.")
    public CustomerResource getById(
            @PathVariable @Parameter(description = OpenApiDocs.CUSTOMER_ID_DESCRIPTION) UUID customerId) {
        return assembler.toResource(
                getCustomer.handle(new GetCustomerQuery(CustomerId.of(customerId))),
                ApiPaths.baseUrl());
    }

    @GetMapping
    @Operation(
            operationId = "listCustomers",
            summary = "List customers",
            description = """
                    Paginated. Each row carries `accountCount`, which is computed by the read model — \
                    the Customer aggregate has no such field, because accounts are opened by a \
                    different module and it could not keep the number correct.

                    Sortable fields: `registeredAt`, `lastName`, `email`. Unknown values fall back to \
                    `registeredAt`.""")
    @ApiResponse(responseCode = "200",
            description = "A page of customers with `page` metadata and navigation links.")
    public PagedModel<CustomerResource> list(PageQuery page) {
        String baseUrl = ApiPaths.baseUrl();
        PageResult<CustomerSummary> found = listCustomers.handle(
                new ListCustomersQuery(page.toPageRequest(DEFAULT_SORT)));

        List<CustomerResource> resources = found.items().stream()
                .map(summary -> assembler.toResource(summary, baseUrl))
                .toList();
        return PagedResources.of(found, resources, ApiPaths.customers(baseUrl));
    }
}
