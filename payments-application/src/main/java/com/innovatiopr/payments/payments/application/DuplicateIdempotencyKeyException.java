package com.innovatiopr.payments.payments.application;

/**
 * Signals that another request already claimed this idempotency key.
 *
 * <p>A distinct exception type on purpose. By the time it is thrown the surrounding
 * database transaction is already doomed — PostgreSQL marks a transaction as aborted after a constraint
 * violation, so nothing further can be read or written on that connection. Unwinding the stack is the only
 * correct response; the caller retries the lookup in a fresh transaction. Compare with
 * {@code TransferErrors.idempotencyKeyReused(...)}, which is the ordinary, non-racing case. Keeping
 * the two apart matters: {@code TransferMoneyHandler} catches <em>this</em> type by name to trigger a
 * replay lookup, so it must never become a subtype of {@code ConflictException} or {@code DomainException}.
 */
public class DuplicateIdempotencyKeyException extends RuntimeException {

    private final String key;

    public DuplicateIdempotencyKeyException(String key) {
        super("Idempotency key already claimed: " + key);
        this.key = key;
    }

    public String key() {
        return key;
    }
}
