package com.innovatiopr.payments.payments.domain;

import com.innovatiopr.payments.shared.domain.DomainException;
import com.innovatiopr.payments.shared.domain.NotFoundException;

/** Expected business failures relating to a payment transaction's own lifecycle. */
public final class TransactionErrors {

    private TransactionErrors() {
    }

    public static NotFoundException notFound(String id) {
        return new NotFoundException("TRANSACTION_NOT_FOUND", "No transaction exists with id " + id);
    }

    public static DomainException invalidTransition(TransactionId id,
                                                    TransactionStatus from,
                                                    TransactionStatus to) {
        return new DomainException(
                "TRANSACTION_INVALID_STATE_TRANSITION",
                "Transaction %s cannot move from %s to %s".formatted(id, from, to));
    }
}
