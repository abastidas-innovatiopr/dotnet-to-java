package com.innovatiopr.payments.customers.infrastructure;

import com.innovatiopr.payments.customers.CustomerId;
import com.innovatiopr.payments.customers.application.CustomerRepository;
import com.innovatiopr.payments.customers.domain.Customer;
import com.innovatiopr.payments.customers.domain.EmailAddress;
import com.innovatiopr.payments.shared.infrastructure.GenericHibernateRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * JPA-backed implementation of the Customers persistence port.
 *
 * <p>Inherits the generic {@code EntityManager} plumbing and adds the operations the domain actually
 * asked for. Note that {@code GenericHibernateRepository} is a superclass here but appears nowhere in
 * {@link CustomerRepository} — the application layer sees only the domain-oriented contract.
 */
@Repository
class HibernateCustomerRepository extends GenericHibernateRepository<CustomerJpaEntity, UUID>
        implements CustomerRepository {

    HibernateCustomerRepository() {
        super(CustomerJpaEntity.class);
    }

    @Override
    public Optional<Customer> findById(CustomerId id) {
        return findById(id.value()).map(CustomerPersistenceMapper::toDomain);
    }

    @Override
    public boolean existsById(CustomerId id) {
        return existsById(id.value());
    }

    @Override
    public boolean existsByEmail(EmailAddress email) {
        return existsBy("email", email.value());
    }

    @Override
    public void save(Customer customer) {
        Optional<CustomerJpaEntity> existing = findById(customer.id().value());
        if (existing.isPresent()) {
            CustomerPersistenceMapper.applyTo(existing.get(), customer);
        } else {
            persist(CustomerPersistenceMapper.toNewEntity(customer));
        }
    }
}
