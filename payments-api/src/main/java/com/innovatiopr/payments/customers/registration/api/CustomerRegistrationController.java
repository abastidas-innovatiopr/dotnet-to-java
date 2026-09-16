package com.innovatiopr.payments.customers.registration.api;

import com.innovatiopr.payments.customers.directory.api.CustomerResource;
import com.innovatiopr.payments.customers.directory.api.CustomerResourceAssembler;
import com.innovatiopr.payments.customers.directory.application.CustomerDetails;
import com.innovatiopr.payments.customers.registration.application.CreateCustomerCommand;
import com.innovatiopr.payments.customers.registration.application.CreateCustomerHandler;
import com.innovatiopr.payments.customers.registration.application.CreateCustomerResult;
import com.innovatiopr.payments.shared.api.ApiPaths;
import com.innovatiopr.payments.shared.api.ApiResponses;
import com.innovatiopr.payments.shared.api.docs.OpenApiDocs;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.hateoas.MediaTypes;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

/**
 * Customer registration.
 *
 * <p>The controller does four things and no more: accept a validated body, build a command, invoke the
 * handler, and assemble the hypermedia response. It enforces no business rule, touches no repository and
 * knows nothing about transactions — a failure simply leaves as an exception and
 * {@code ApiExceptionHandler} turns it into a problem document.
 */
@RestController
@RequestMapping(path = ApiPaths.CUSTOMERS, produces = MediaTypes.HAL_JSON_VALUE)
@Tag(name = OpenApiDocs.TAG_CUSTOMERS)
public class CustomerRegistrationController {

    private final CreateCustomerHandler createCustomer;
    private final CustomerResourceAssembler assembler;

    CustomerRegistrationController(CreateCustomerHandler createCustomer,
                                   CustomerResourceAssembler assembler) {
        this.createCustomer = createCustomer;
        this.assembler = assembler;
    }

    @PostMapping
    @Operation(
            operationId = "createCustomer",
            summary = "Register a customer",
            description = """
                    Registers a customer. The email must be unique across all customers — a rule about \
                    the set rather than about one customer, which is why it lives in the handler and \
                    not in the aggregate.""")
    @ApiResponse(responseCode = "201",
            description = "Customer registered. `Location` points at the new resource.")
    @ApiResponse(responseCode = "400", description = "Invalid body — see the `errors` array.")
    @ApiResponse(responseCode = "409", description = "`CUSTOMER_EMAIL_ALREADY_REGISTERED`.")
    public ResponseEntity<CustomerResource> create(@Valid @RequestBody CreateCustomerRequest body) {
        CreateCustomerResult created = createCustomer.handle(
                new CreateCustomerCommand(body.firstName(), body.lastName(), body.email()));

        String baseUrl = ApiPaths.baseUrl();
        return ApiResponses.created(
                assembler.toResource(toDetails(created), baseUrl),
                URI.create(ApiPaths.customer(baseUrl, created.customerId())));
    }

    private static CustomerDetails toDetails(CreateCustomerResult result) {
        return new CustomerDetails(result.customerId(), result.firstName(), result.lastName(),
                result.email(), result.registeredAt());
    }
}
