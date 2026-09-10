package com.innovatiopr.payments.payments.transfer.api;

import com.innovatiopr.payments.payments.transfer.application.TransferMoneyResult;
import com.innovatiopr.payments.shared.api.ApiPaths;
import org.springframework.hateoas.IanaLinkRelations;
import org.springframework.hateoas.Link;
import org.springframework.stereotype.Component;

@Component
public class TransferResourceAssembler {

    public TransferResource toResource(TransferMoneyResult result, String baseUrl) {
        TransferResource resource = new TransferResource(result.transactionId(), result.sourceAccountId(),
                result.destinationAccountId(), result.amount(), result.currency(), result.status(),
                result.reference(), result.sourceBalanceAfter(), result.destinationBalanceAfter(),
                result.completedAt(), result.replayed());

        // Self points at the transaction resource: that is where this transfer can be re-read.
        resource.add(Link.of(ApiPaths.transaction(baseUrl, result.transactionId()), IanaLinkRelations.SELF));
        resource.add(Link.of(ApiPaths.transaction(baseUrl, result.transactionId()), "transaction"));
        if (result.sourceAccountId() != null) {
            resource.add(Link.of(ApiPaths.account(baseUrl, result.sourceAccountId()), "sourceAccount"));
        }
        if (result.destinationAccountId() != null) {
            resource.add(Link.of(ApiPaths.account(baseUrl, result.destinationAccountId()),
                    "destinationAccount"));
        }
        return resource;
    }
}
