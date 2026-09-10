package com.innovatiopr.payments.payments.transfer.application;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * @param replayed true when this response was served from a stored idempotency record rather than by
 *                 moving money. The API layer uses it to replay the original status code.
 */
public record TransferMoneyResult(UUID transactionId, UUID sourceAccountId, UUID destinationAccountId,
                                  BigDecimal amount, String currency, String status, String reference,
                                  BigDecimal sourceBalanceAfter, BigDecimal destinationBalanceAfter,
                                  Instant completedAt, boolean replayed) { }
