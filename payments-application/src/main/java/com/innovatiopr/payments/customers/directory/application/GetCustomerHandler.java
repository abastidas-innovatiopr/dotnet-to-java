package com.innovatiopr.payments.customers.directory.application;

import com.innovatiopr.payments.customers.domain.CustomerErrors;
import com.innovatiopr.payments.shared.application.QueryHandler;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GetCustomerHandler implements QueryHandler<GetCustomerQuery, CustomerDetails> {

    private final CustomerReadModel customers;

    public GetCustomerHandler(CustomerReadModel customers) {
        this.customers = customers;
    }

    @Override
    @Transactional(readOnly = true)
    public CustomerDetails handle(GetCustomerQuery query) {
        return customers.findById(query.customerId())
                .orElseThrow(() -> CustomerErrors.notFound(query.customerId()));
    }
}
