package com.innovatiopr.payments.payments.domain;

import com.innovatiopr.payments.accounts.AccountId;
import com.innovatiopr.payments.shared.domain.DomainError;
import com.innovatiopr.payments.shared.domain.ErrorType;

/** Expected business failures when initiating a payment. */
public sealed interface TransferError extends DomainError {

    record SameAccount(AccountId accountId) implements TransferError {
        @Override
        public String code() {
            return "TRANSFER_SAME_ACCOUNT";
        }

        @Override
        public String message() {
            return "Source and destination accounts must differ (both were " + accountId.value() + ")";
        }

        @Override
        public ErrorType type() {
            return ErrorType.BUSINESS_RULE;
        }
    }

    record MissingIdempotencyKey() implements TransferError {
        @Override
        public String code() {
            return "TRANSFER_IDEMPOTENCY_KEY_REQUIRED";
        }

        @Override
        public String message() {
            return "An Idempotency-Key header is required for money-moving requests";
        }

        @Override
        public ErrorType type() {
            return ErrorType.VALIDATION;
        }
    }

    record InvalidIdempotencyKey(String reason) implements TransferError {
        @Override
        public String code() {
            return "TRANSFER_IDEMPOTENCY_KEY_INVALID";
        }

        @Override
        public String message() {
            return reason;
        }

        @Override
        public ErrorType type() {
            return ErrorType.VALIDATION;
        }
    }

    /**
     * The same idempotency key was replayed with a different request body. Returning the original result
     * would be wrong (it answers a different question) and processing the new body would break the
     * promise the key encodes, so the only safe answer is to refuse.
     */
    record IdempotencyKeyReused(String key) implements TransferError {
        @Override
        public String code() {
            return "TRANSFER_IDEMPOTENCY_KEY_REUSED";
        }

        @Override
        public String message() {
            return "Idempotency key '" + key + "' was already used for a different request";
        }

        @Override
        public ErrorType type() {
            return ErrorType.CONFLICT;
        }
    }

    record InvalidReference(String reason) implements TransferError {
        @Override
        public String code() {
            return "TRANSFER_INVALID_REFERENCE";
        }

        @Override
        public String message() {
            return reason;
        }

        @Override
        public ErrorType type() {
            return ErrorType.VALIDATION;
        }
    }

    static TransferError sameAccount(AccountId id) {
        return new SameAccount(id);
    }

    static TransferError missingIdempotencyKey() {
        return new MissingIdempotencyKey();
    }

    static TransferError invalidIdempotencyKey(String reason) {
        return new InvalidIdempotencyKey(reason);
    }

    static TransferError idempotencyKeyReused(String key) {
        return new IdempotencyKeyReused(key);
    }

    static TransferError invalidReference(String reason) {
        return new InvalidReference(reason);
    }
}
