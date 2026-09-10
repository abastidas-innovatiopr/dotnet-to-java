package com.innovatiopr.payments.payments.application;

import com.innovatiopr.payments.payments.domain.PaymentTransaction;
import com.innovatiopr.payments.payments.domain.TransactionId;

import java.util.Optional;

/** The Payments module's persistence port for the {@code PaymentTransaction} aggregate. */
public interface PaymentTransactionRepository {

    Optional<PaymentTransaction> findById(TransactionId id);

    void save(PaymentTransaction transaction);
}
