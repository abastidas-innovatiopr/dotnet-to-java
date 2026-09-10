package com.innovatiopr.payments.payments.infrastructure;

import com.innovatiopr.payments.payments.application.DuplicateIdempotencyKeyException;
import com.innovatiopr.payments.payments.application.IdempotencyStore;
import com.innovatiopr.payments.payments.domain.IdempotencyKey;
import com.innovatiopr.payments.payments.domain.IdempotencyRecord;
import com.innovatiopr.payments.payments.domain.TransactionId;
import com.innovatiopr.payments.shared.infrastructure.GenericHibernateRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Stores idempotency records, translating a unique-constraint violation into a domain-meaningful signal.
 *
 * <p>{@code flush()} is called explicitly after {@code persist}. Without it the {@code INSERT} would be
 * deferred to the end of the transaction, and the violation would surface during commit — far away from
 * the code that knows what it means, and too late for the handler to distinguish a duplicate key from any
 * other commit failure. Flushing brings the failure to the point where it can be interpreted.
 */
@Repository
class JpaIdempotencyStore extends GenericHibernateRepository<IdempotencyRecordJpaEntity, String>
        implements IdempotencyStore {

    JpaIdempotencyStore() {
        super(IdempotencyRecordJpaEntity.class);
    }

    @Override
    protected String idAttributeName() {
        return "idempotencyKey";
    }

    @Override
    public Optional<IdempotencyRecord> find(IdempotencyKey key) {
        return findById(key.value()).map(JpaIdempotencyStore::toDomain);
    }

    @Override
    public void save(IdempotencyRecord record) {
        try {
            persist(new IdempotencyRecordJpaEntity(
                    record.key().value(),
                    record.requestHash(),
                    record.transactionId().value(),
                    record.responseStatus(),
                    record.createdAt()));
            flush();
        } catch (DataIntegrityViolationException | jakarta.persistence.PersistenceException e) {
            throw new DuplicateIdempotencyKeyException(record.key().value());
        }
    }

    private static IdempotencyRecord toDomain(IdempotencyRecordJpaEntity entity) {
        return new IdempotencyRecord(
                IdempotencyKey.fromStorage(entity.getIdempotencyKey()),
                entity.getRequestHash(),
                TransactionId.of(entity.getTransactionId()),
                entity.getResponseStatus(),
                entity.getCreatedAt());
    }
}
