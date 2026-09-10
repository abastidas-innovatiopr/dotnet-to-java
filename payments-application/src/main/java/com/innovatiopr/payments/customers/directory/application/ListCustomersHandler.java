package com.innovatiopr.payments.customers.directory.application;

import com.innovatiopr.payments.shared.application.PageResult;
import com.innovatiopr.payments.shared.application.QueryHandler;
import com.innovatiopr.payments.shared.domain.Result;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ListCustomersHandler implements QueryHandler<ListCustomersQuery, PageResult<CustomerSummary>> {

    private final CustomerReadModel customers;

    public ListCustomersHandler(CustomerReadModel customers) {
        this.customers = customers;
    }

    @Override
    @Transactional(readOnly = true)
    public Result<PageResult<CustomerSummary>> handle(ListCustomersQuery query) {
        return Result.success(customers.list(query.page()));
    }
}
