package com.innovatiopr.payments.payments.transfer.application;

import com.innovatiopr.payments.payments.application.IdempotencyStore;
import com.innovatiopr.payments.payments.application.PaymentTransactionRepository;
import com.innovatiopr.payments.payments.domain.IdempotencyKey;
import com.innovatiopr.payments.payments.domain.IdempotencyRecord;
import com.innovatiopr.payments.payments.domain.PaymentTransaction;
import com.innovatiopr.payments.payments.domain.TransferError;
import com.innovatiopr.payments.shared.domain.Result;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Reads a stored idempotency outcome in a transaction of its own.
 *
 * <p>This is a separate bean for the reason spelled out in {@link TransferMoneyOperation}: Spring's
 * {@code @Transactional} is proxy-based, so annotating a method that {@link TransferMoneyHandler} calls on
 * <em>itself</em> would have no effect whatsoever. The annotation only applies when the call arrives
 * through the proxy — that is, from a different bean.
 *
 * <p>{@code REQUIRES_NEW} rather than the default: after a duplicate-key violation the caller's
 * transaction is aborted, and any statement issued on it fails. A genuinely new transaction is the only
 * way to read the winning record.
 */
@Service
class TransferReplayReader {

    private final IdempotencyStore idempotency;
    private final PaymentTransactionRepository transactions;

    TransferReplayReader(IdempotencyStore idempotency, PaymentTransactionRepository transactions) {
        this.idempotency = idempotency;
        this.transactions = transactions;
    }

    @Transactional(readOnly = true, propagation = Propagation.REQUIRES_NEW)
    Optional<IdempotencyRecord> find(IdempotencyKey key) {
        return idempotency.find(key);
    }

    /** Rebuilds the original response, or reports that the key was reused for a different request. */
    @Transactional(readOnly = true, propagation = Propagation.REQUIRES_NEW)
    Result<TransferMoneyResult> replay(IdempotencyRecord record, String requestHash) {
        if (!record.matches(requestHash)) {
            return Result.failure(TransferError.idempotencyKeyReused(record.key().value()));
        }

        Optional<PaymentTransaction> original = transactions.findById(record.transactionId());
        if (original.isEmpty()) {
            // The record and the transaction commit together, so this is unreachable without data loss.
            throw new IllegalStateException("Idempotency record " + record.key()
                    + " references missing transaction " + record.transactionId());
        }

        PaymentTransaction transaction = original.get();
        return Result.success(new TransferMoneyResult(
                transaction.id().value(),
                transaction.sourceAccountId().map(id -> id.value()).orElse(null),
                transaction.destinationAccountId().map(id -> id.value()).orElse(null),
                transaction.amount().amount(),
                transaction.amount().currency().getCurrencyCode(),
                transaction.status().name(),
                transaction.reference().value(),
                null,
                null,
                transaction.completedAt().orElse(transaction.createdAt()),
                true));
    }
}
