package com.innovatiopr.payments.payments.domain;

import java.time.Instant;
import java.util.Objects;

/**
 * The stored outcome of a money-moving request, keyed by its {@link IdempotencyKey}.
 *
 * @param key            the client-supplied key
 * @param requestHash    SHA-256 of the canonical request body, so a replayed key carrying a
 *                       <em>different</em> request can be detected and refused
 * @param transactionId  the transaction the first attempt produced
 * @param responseStatus the HTTP status the first attempt returned, replayed verbatim on retry
 * @param createdAt      when the first attempt committed
 */
public record IdempotencyRecord(IdempotencyKey key, String requestHash, TransactionId transactionId,
                                int responseStatus, Instant createdAt) {

    public IdempotencyRecord {
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(requestHash, "requestHash");
        Objects.requireNonNull(transactionId, "transactionId");
        Objects.requireNonNull(createdAt, "createdAt");
    }

    public boolean matches(String otherRequestHash) {
        return requestHash.equals(otherRequestHash);
    }
}
