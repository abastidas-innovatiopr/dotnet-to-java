package com.innovatiopr.payments.payments.domain;

import com.innovatiopr.payments.shared.domain.DomainError;
import com.innovatiopr.payments.shared.domain.ErrorType;

/** Expected business failures relating to a payment transaction's own lifecycle. */
public sealed interface TransactionError extends DomainError {

    record NotFound(String transactionId) implements TransactionError {
        @Override
        public String code() {
            return "TRANSACTION_NOT_FOUND";
        }

        @Override
        public String message() {
            return "No transaction exists with id " + transactionId;
        }

        @Override
        public ErrorType type() {
            return ErrorType.NOT_FOUND;
        }
    }

    record InvalidStateTransition(String transactionId, TransactionStatus from, TransactionStatus to)
            implements TransactionError {
        @Override
        public String code() {
            return "TRANSACTION_INVALID_STATE_TRANSITION";
        }

        @Override
        public String message() {
            return "Transaction %s cannot move from %s to %s".formatted(transactionId, from, to);
        }

        @Override
        public ErrorType type() {
            return ErrorType.BUSINESS_RULE;
        }
    }

    static TransactionError notFound(String id) {
        return new NotFound(id);
    }

    static TransactionError invalidTransition(TransactionId id, TransactionStatus from, TransactionStatus to) {
        return new InvalidStateTransition(id.toString(), from, to);
    }
}
