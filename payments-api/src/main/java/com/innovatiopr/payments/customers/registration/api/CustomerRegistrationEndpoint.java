package com.innovatiopr.payments.customers.registration.api;

import com.innovatiopr.payments.customers.directory.api.CustomerResourceAssembler;
import com.innovatiopr.payments.customers.directory.application.CustomerDetails;
import com.innovatiopr.payments.customers.registration.application.CreateCustomerCommand;
import com.innovatiopr.payments.customers.registration.application.CreateCustomerHandler;
import com.innovatiopr.payments.customers.registration.application.CreateCustomerResult;
import com.innovatiopr.payments.shared.api.ApiPaths;
import com.innovatiopr.payments.shared.api.ApiResponses;
import com.innovatiopr.payments.shared.api.RequestValidator;
import com.innovatiopr.payments.shared.domain.Result;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.function.ServerRequest;
import org.springframework.web.servlet.function.ServerResponse;

import java.net.URI;
import java.util.Optional;

/**
 * Handler functions for customer registration.
 *
 * <p>The endpoint does six things and no more: read the body, validate transport constraints, build a
 * command, invoke the handler, map the result to a status, and assemble the hypermedia response. It
 * enforces no business rule, touches no repository and knows nothing about transactions.
 */
@Component
class CustomerRegistrationEndpoint {

    private final CreateCustomerHandler createCustomer;
    private final CustomerResourceAssembler assembler;
    private final RequestValidator validator;

    CustomerRegistrationEndpoint(CreateCustomerHandler createCustomer, CustomerResourceAssembler assembler,
                                 RequestValidator validator) {
        this.createCustomer = createCustomer;
        this.assembler = assembler;
        this.validator = validator;
    }

    ServerResponse create(ServerRequest request) throws Exception {
        CreateCustomerRequest body = request.body(CreateCustomerRequest.class);

        Optional<ServerResponse> invalid = validator.validate(body);
        if (invalid.isPresent()) {
            return invalid.get();
        }

        Result<CreateCustomerResult> result = createCustomer.handle(
                new CreateCustomerCommand(body.firstName(), body.lastName(), body.email()));

        String baseUrl = ApiPaths.baseUrl(request);
        return ApiResponses.created(result,
                created -> assembler.toResource(toDetails(created), baseUrl),
                created -> URI.create(ApiPaths.customer(baseUrl, created.customerId())));
    }

    private static CustomerDetails toDetails(CreateCustomerResult result) {
        return new CustomerDetails(result.customerId(), result.firstName(), result.lastName(),
                result.email(), result.registeredAt());
    }
}
