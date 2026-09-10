package com.innovatiopr.payments.ledger.statements.api;

import com.innovatiopr.payments.ledger.statements.application.StatementLine;
import com.innovatiopr.payments.shared.api.ApiPaths;
import org.springframework.hateoas.Link;
import org.springframework.stereotype.Component;

@Component
public class StatementResourceAssembler {

    public StatementLineResource toResource(StatementLine line, String baseUrl) {
        StatementLineResource resource = new StatementLineResource(line.entryId(), line.ledgerTransactionId(),
                line.postingReference(), line.direction(), line.amount(), line.currency(),
                line.runningBalance(), line.description(), line.recordedAt());

        // The posting reference is the Ledger's copy of the Payments transaction id, so a statement line
        // can link back to the operation that produced it without the Ledger importing that module.
        resource.add(Link.of(ApiPaths.transaction(baseUrl, line.postingReference()), "transaction"));
        return resource;
    }
}
