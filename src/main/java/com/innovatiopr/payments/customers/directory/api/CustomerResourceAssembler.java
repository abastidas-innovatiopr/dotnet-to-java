package com.innovatiopr.payments.customers.directory.api;

import com.innovatiopr.payments.customers.directory.application.CustomerDetails;
import com.innovatiopr.payments.customers.directory.application.CustomerSummary;
import com.innovatiopr.payments.shared.api.ApiPaths;
import org.springframework.hateoas.IanaLinkRelations;
import org.springframework.hateoas.Link;
import org.springframework.stereotype.Component;

/**
 * Builds {@link CustomerResource} instances and their links.
 *
 * <p>A dedicated component rather than inline construction in the endpoint: link structure is part of the
 * API contract and belongs somewhere it can be tested on its own and reused by every endpoint that
 * returns a customer.
 */
@Component
public class CustomerResourceAssembler {

    public CustomerResource toResource(CustomerDetails details, String baseUrl) {
        CustomerResource resource = new CustomerResource(details.id(), details.firstName(), details.lastName(),
                details.email(), details.registeredAt(), null);
        resource.add(Link.of(ApiPaths.customer(baseUrl, details.id()), IanaLinkRelations.SELF));
        resource.add(Link.of(ApiPaths.accounts(baseUrl) + "?customerId=" + details.id(), "accounts"));
        return resource;
    }

    public CustomerResource toResource(CustomerSummary summary, String baseUrl) {
        String[] parts = summary.fullName().split(" ", 2);
        CustomerResource resource = new CustomerResource(summary.id(), parts[0],
                parts.length > 1 ? parts[1] : "", summary.email(), summary.registeredAt(),
                summary.accountCount());
        resource.add(Link.of(ApiPaths.customer(baseUrl, summary.id()), IanaLinkRelations.SELF));
        return resource;
    }
}
