package com.innovatiopr.payments.payments.infrastructure;

import com.innovatiopr.payments.accounts.AccountId;
import com.innovatiopr.payments.payments.domain.PaymentTransaction;
import com.innovatiopr.payments.payments.domain.TransactionId;
import com.innovatiopr.payments.payments.domain.TransactionReference;
import com.innovatiopr.payments.payments.domain.TransactionStatus;
import com.innovatiopr.payments.payments.domain.TransactionType;
import com.innovatiopr.payments.shared.domain.Money;

import java.util.Currency;

final class PaymentTransactionPersistenceMapper {

    private PaymentTransactionPersistenceMapper() {
    }

    static PaymentTransaction toDomain(PaymentTransactionJpaEntity entity) {
        Currency currency = Currency.getInstance(entity.getCurrency());
        return PaymentTransaction.reconstitute(
                TransactionId.of(entity.getId()),
                TransactionType.valueOf(entity.getType()),
                entity.getSourceAccountId() == null ? null : AccountId.of(entity.getSourceAccountId()),
                entity.getDestinationAccountId() == null ? null : AccountId.of(entity.getDestinationAccountId()),
                Money.of(entity.getAmount(), currency),
                TransactionReference.fromStorage(entity.getReference()),
                TransactionStatus.valueOf(entity.getStatus()),
                entity.getCreatedAt(),
                entity.getCompletedAt(),
                entity.getFailureCode());
    }

    static PaymentTransactionJpaEntity toNewEntity(PaymentTransaction transaction) {
        return new PaymentTransactionJpaEntity(
                transaction.id().value(),
                transaction.type().name(),
                transaction.sourceAccountId().map(AccountId::value).orElse(null),
                transaction.destinationAccountId().map(AccountId::value).orElse(null),
                transaction.amount().amount(),
                transaction.amount().currency().getCurrencyCode(),
                transaction.status().name(),
                transaction.reference().value(),
                transaction.createdAt(),
                transaction.completedAt().orElse(null),
                transaction.failureCode().orElse(null));
    }

    static void applyTo(PaymentTransactionJpaEntity entity, PaymentTransaction transaction) {
        entity.setStatus(transaction.status().name());
        entity.setReference(transaction.reference().value());
        entity.setCompletedAt(transaction.completedAt().orElse(null));
        entity.setFailureCode(transaction.failureCode().orElse(null));
    }
}
