package com.innovatiopr.payments.customers.infrastructure;

import com.innovatiopr.payments.customers.CustomerId;
import com.innovatiopr.payments.customers.domain.Customer;
import com.innovatiopr.payments.customers.domain.EmailAddress;
import com.innovatiopr.payments.customers.domain.PersonName;

/** Translates between the {@code Customer} aggregate and its JPA representation. */
final class CustomerPersistenceMapper {

    private CustomerPersistenceMapper() {
    }

    static Customer toDomain(CustomerJpaEntity entity) {
        return Customer.reconstitute(
                CustomerId.of(entity.getId()),
                PersonName.fromStorage(entity.getFirstName(), entity.getLastName()),
                EmailAddress.fromStorage(entity.getEmail()),
                entity.getRegisteredAt(),
                entity.getVersion());
    }

    static CustomerJpaEntity toNewEntity(Customer customer) {
        return new CustomerJpaEntity(
                customer.id().value(),
                customer.name().firstName(),
                customer.name().lastName(),
                customer.email().value(),
                customer.registeredAt(),
                customer.version());
    }

    /** Copies mutable state onto a managed entity, leaving Hibernate to detect the change. */
    static void applyTo(CustomerJpaEntity entity, Customer customer) {
        entity.setFirstName(customer.name().firstName());
        entity.setLastName(customer.name().lastName());
        entity.setEmail(customer.email().value());
    }
}
