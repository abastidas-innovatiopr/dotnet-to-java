package com.innovatiopr.payments.payments.application;

import com.innovatiopr.payments.payments.domain.IdempotencyKey;
import com.innovatiopr.payments.payments.domain.IdempotencyRecord;

import java.util.Optional;

/** Persistence port for idempotency records. */
public interface IdempotencyStore {

    Optional<IdempotencyRecord> find(IdempotencyKey key);

    /**
     * Inserts the record.
     *
     * <p>Reading first and then inserting is not sufficient on its own: two requests carrying the same key
     * can both find nothing and both proceed. The unique index on {@code idempotency_records.idempotency_key}
     * is what actually prevents double spending — the second inserter blocks on the index until the first
     * commits and then fails. Correctness comes from the constraint; the prior read is only an optimisation
     * that avoids doing work we know will be discarded.
     *
     * @throws DuplicateIdempotencyKeyException when a record already exists for this key
     */
    void save(IdempotencyRecord record);
}
