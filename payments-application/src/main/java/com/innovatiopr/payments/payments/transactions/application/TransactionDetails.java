package com.innovatiopr.payments.payments.transactions.application;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/** Read model for a payment transaction. */
public record TransactionDetails(UUID id, String type, UUID sourceAccountId, UUID destinationAccountId,
                                 BigDecimal amount, String currency, String status, String reference,
                                 Instant createdAt, Instant completedAt, String failureCode) { }
