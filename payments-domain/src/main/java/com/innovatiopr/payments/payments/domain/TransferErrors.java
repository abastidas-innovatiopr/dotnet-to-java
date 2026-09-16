package com.innovatiopr.payments.payments.domain;

import com.innovatiopr.payments.accounts.AccountId;
import com.innovatiopr.payments.shared.domain.ConflictException;
import com.innovatiopr.payments.shared.domain.DomainException;
import com.innovatiopr.payments.shared.domain.ValidationException;

/** Expected business failures when initiating a payment. */
public final class TransferErrors {

    private TransferErrors() {
    }

    public static DomainException sameAccount(AccountId id) {
        return new DomainException(
                "TRANSFER_SAME_ACCOUNT",
                "Source and destination accounts must differ (both were " + id.value() + ")");
    }

    public static ValidationException missingIdempotencyKey() {
        return new ValidationException(
                "TRANSFER_IDEMPOTENCY_KEY_REQUIRED",
                "An Idempotency-Key header is required for money-moving requests");
    }

    public static ValidationException invalidIdempotencyKey(String reason) {
        return new ValidationException("TRANSFER_IDEMPOTENCY_KEY_INVALID", reason);
    }

    /**
     * The same idempotency key was replayed with a different request body. Returning the original result
     * would be wrong (it answers a different question) and processing the new body would break the
     * promise the key encodes, so the only safe answer is to refuse.
     */
    public static ConflictException idempotencyKeyReused(String key) {
        return new ConflictException(
                "TRANSFER_IDEMPOTENCY_KEY_REUSED",
                "Idempotency key '" + key + "' was already used for a different request");
    }

    public static ValidationException invalidReference(String reason) {
        return new ValidationException("TRANSFER_INVALID_REFERENCE", reason);
    }
}
