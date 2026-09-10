package com.innovatiopr.payments.payments.deposits.application;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/** Outcome of a deposit or a withdrawal. */
public record CashMovementResult(UUID transactionId, UUID accountId, BigDecimal amount, String currency,
                                 String status, BigDecimal balanceAfter, Instant completedAt) { }
