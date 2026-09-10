package com.innovatiopr.payments.customers.directory.application;

import com.innovatiopr.payments.customers.domain.CustomerError;
import com.innovatiopr.payments.shared.application.QueryHandler;
import com.innovatiopr.payments.shared.domain.Result;
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
    public Result<CustomerDetails> handle(GetCustomerQuery query) {
        return customers.findById(query.customerId())
                .map(Result::success)
                .orElseGet(() -> Result.failure(CustomerError.notFound(query.customerId())));
    }
}
