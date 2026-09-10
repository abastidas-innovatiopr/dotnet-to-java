package com.innovatiopr.payments.payments.infrastructure;

import com.innovatiopr.payments.payments.application.PaymentTransactionRepository;
import com.innovatiopr.payments.payments.domain.PaymentTransaction;
import com.innovatiopr.payments.payments.domain.TransactionId;
import com.innovatiopr.payments.shared.infrastructure.GenericHibernateRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
class HibernatePaymentTransactionRepository
        extends GenericHibernateRepository<PaymentTransactionJpaEntity, UUID>
        implements PaymentTransactionRepository {

    HibernatePaymentTransactionRepository() {
        super(PaymentTransactionJpaEntity.class);
    }

    @Override
    public Optional<PaymentTransaction> findById(TransactionId id) {
        return findById(id.value()).map(PaymentTransactionPersistenceMapper::toDomain);
    }

    @Override
    public void save(PaymentTransaction transaction) {
        Optional<PaymentTransactionJpaEntity> existing = findById(transaction.id().value());
        if (existing.isPresent()) {
            PaymentTransactionPersistenceMapper.applyTo(existing.get(), transaction);
        } else {
            persist(PaymentTransactionPersistenceMapper.toNewEntity(transaction));
        }
    }
}
