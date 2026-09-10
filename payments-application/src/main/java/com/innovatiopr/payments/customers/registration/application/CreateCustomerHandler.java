package com.innovatiopr.payments.customers.registration.application;

import com.innovatiopr.payments.customers.CustomerId;
import com.innovatiopr.payments.customers.application.CustomerRepository;
import com.innovatiopr.payments.customers.domain.Customer;
import com.innovatiopr.payments.customers.domain.CustomerError;
import com.innovatiopr.payments.customers.domain.EmailAddress;
import com.innovatiopr.payments.shared.application.CommandHandler;
import com.innovatiopr.payments.shared.application.DomainEventPublisher;
import com.innovatiopr.payments.shared.domain.Result;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;

/**
 * Registers a customer.
 *
 * <p>A handler orchestrates; it does not decide. Whether the email is well formed is
 * {@code EmailAddress}'s business and whether the name is acceptable is {@code PersonName}'s. What lives
 * here is the part no single aggregate can know: that this email is not already taken by a
 * <em>different</em> customer — a rule about the set of all customers, not about one of them.
 */
@Service
@Transactional
public class CreateCustomerHandler implements CommandHandler<CreateCustomerCommand, CreateCustomerResult> {

    private final CustomerRepository customers;
    private final DomainEventPublisher events;
    private final Clock clock;

    public CreateCustomerHandler(CustomerRepository customers, DomainEventPublisher events, Clock clock) {
        this.customers = customers;
        this.events = events;
        this.clock = clock;
    }

    @Override
    public Result<CreateCustomerResult> handle(CreateCustomerCommand command) {
        Result<EmailAddress> email = EmailAddress.create(command.email());
        if (email.isFailure()) {
            return email.propagate();
        }
        if (customers.existsByEmail(email.orElseThrow())) {
            return Result.failure(CustomerError.emailAlreadyRegistered(email.orElseThrow().value()));
        }

        Result<Customer> created = Customer.register(CustomerId.generate(), command.firstName(),
                command.lastName(), command.email(), clock.instant());
        if (created.isFailure()) {
            return created.propagate();
        }

        Customer customer = created.orElseThrow();
        customers.save(customer);
        events.publishFrom(customer);

        return Result.success(new CreateCustomerResult(customer.id().value(), customer.name().firstName(),
                customer.name().lastName(), customer.email().value(), customer.registeredAt()));
    }
}
