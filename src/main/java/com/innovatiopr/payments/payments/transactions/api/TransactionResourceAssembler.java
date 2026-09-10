package com.innovatiopr.payments.payments.transactions.api;

import com.innovatiopr.payments.payments.transactions.application.TransactionDetails;
import com.innovatiopr.payments.shared.api.ApiPaths;
import org.springframework.hateoas.IanaLinkRelations;
import org.springframework.hateoas.Link;
import org.springframework.stereotype.Component;

@Component
public class TransactionResourceAssembler {

    public TransactionResource toResource(TransactionDetails details, String baseUrl) {
        TransactionResource resource = new TransactionResource(details.id(), details.type(),
                details.sourceAccountId(), details.destinationAccountId(), details.amount(),
                details.currency(), details.status(), details.reference(), details.createdAt(),
                details.completedAt(), details.failureCode());

        resource.add(Link.of(ApiPaths.transaction(baseUrl, details.id()), IanaLinkRelations.SELF));
        if (details.sourceAccountId() != null) {
            resource.add(Link.of(ApiPaths.account(baseUrl, details.sourceAccountId()), "sourceAccount"));
        }
        if (details.destinationAccountId() != null) {
            resource.add(Link.of(ApiPaths.account(baseUrl, details.destinationAccountId()),
                    "destinationAccount"));
        }
        return resource;
    }
}
