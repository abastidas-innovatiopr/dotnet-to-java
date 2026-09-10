package com.innovatiopr.payments.ledger.infrastructure;

import com.innovatiopr.payments.ledger.LedgerTransactionId;
import com.innovatiopr.payments.ledger.PostingReference;
import com.innovatiopr.payments.ledger.application.LedgerRepository;
import com.innovatiopr.payments.ledger.domain.LedgerTransaction;
import com.innovatiopr.payments.shared.infrastructure.GenericHibernateRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * JPA-backed implementation of the Ledger persistence port.
 *
 * <p>Append-only: {@code save} always persists a new aggregate. There is no update path, because a
 * ledger that can be edited is not an audit trail. Corrections are made by posting a reversing entry.
 */
@Repository
class HibernateLedgerRepository extends GenericHibernateRepository<LedgerTransactionJpaEntity, UUID>
        implements LedgerRepository {

    HibernateLedgerRepository() {
        super(LedgerTransactionJpaEntity.class);
    }

    @Override
    public Optional<LedgerTransaction> findById(LedgerTransactionId id) {
        return findById(id.value()).map(LedgerPersistenceMapper::toDomain);
    }

    @Override
    public Optional<LedgerTransaction> findByReference(PostingReference reference) {
        return findOneBy("postingReference", reference.value()).map(LedgerPersistenceMapper::toDomain);
    }

    @Override
    public void save(LedgerTransaction transaction) {
        persist(LedgerPersistenceMapper.toNewEntity(transaction));
    }
}
