package com.innovatiopr.payments.customers.application;

import com.innovatiopr.payments.customers.CustomerId;
import com.innovatiopr.payments.customers.CustomersApi;
import com.innovatiopr.payments.customers.domain.CustomerError;
import com.innovatiopr.payments.customers.registration.application.CreateCustomerCommand;
import com.innovatiopr.payments.customers.registration.application.CreateCustomerHandler;
import com.innovatiopr.payments.customers.registration.application.CreateCustomerResult;
import com.innovatiopr.payments.shared.domain.Result;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class CustomersApiAdapter implements CustomersApi {

    private final CustomerRepository customers;
    private final CreateCustomerHandler createCustomer;

    CustomersApiAdapter(CustomerRepository customers, CreateCustomerHandler createCustomer) {
        this.customers = customers;
        this.createCustomer = createCustomer;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean exists(CustomerId customerId) {
        return customers.existsById(customerId);
    }

    @Override
    @Transactional(readOnly = true)
    public Result<Void> requireExists(CustomerId customerId) {
        return customers.existsById(customerId)
                ? Result.ok()
                : Result.failure(CustomerError.notFound(customerId));
    }

    @Override
    public Result<CustomerId> register(String firstName, String lastName, String email) {
        Result<CreateCustomerResult> created = createCustomer.handle(
                new CreateCustomerCommand(firstName, lastName, email));
        return created.map(result -> CustomerId.of(result.customerId()));
    }
}
