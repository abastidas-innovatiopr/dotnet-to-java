package com.innovatiopr.payments.payments.deposits.api;

import com.innovatiopr.payments.payments.deposits.application.CashMovementResult;
import com.innovatiopr.payments.shared.api.ApiPaths;
import org.springframework.hateoas.IanaLinkRelations;
import org.springframework.hateoas.Link;
import org.springframework.stereotype.Component;

@Component
public class CashMovementResourceAssembler {

    public CashMovementResource toResource(CashMovementResult result, String baseUrl) {
        CashMovementResource resource = new CashMovementResource(result.transactionId(), result.accountId(),
                result.amount(), result.currency(), result.status(), result.balanceAfter(),
                result.completedAt());
        resource.add(Link.of(ApiPaths.transaction(baseUrl, result.transactionId()), IanaLinkRelations.SELF));
        resource.add(Link.of(ApiPaths.account(baseUrl, result.accountId()), "account"));
        resource.add(Link.of(ApiPaths.accountBalance(baseUrl, result.accountId()), "balance"));
        resource.add(Link.of(ApiPaths.accountTransactions(baseUrl, result.accountId()), "transactions"));
        return resource;
    }
}
