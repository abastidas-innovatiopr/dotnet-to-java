package com.innovatiopr.payments.customers.application;

import com.innovatiopr.payments.customers.CustomerId;
import com.innovatiopr.payments.customers.CustomersApi;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class CustomersApiAdapter implements CustomersApi {

    private final CustomerRepository customers;

    CustomersApiAdapter(CustomerRepository customers) {
        this.customers = customers;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean exists(CustomerId customerId) {
        return customers.existsById(customerId);
    }
}
