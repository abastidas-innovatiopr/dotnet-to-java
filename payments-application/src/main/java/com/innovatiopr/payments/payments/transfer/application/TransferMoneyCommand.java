package com.innovatiopr.payments.payments.transfer.application;

import com.innovatiopr.payments.accounts.AccountId;
import com.innovatiopr.payments.payments.domain.IdempotencyKey;
import com.innovatiopr.payments.shared.application.Command;

import java.math.BigDecimal;

/**
 * Move money between two accounts.
 *
 * @param requestHash fingerprint of the original HTTP body, computed at the API edge. Replaying a key with
 *                    a different body must be refused, and that comparison needs the body's identity —
 *                    but the application layer should not have to know what JSON is, so the API layer
 *                    hands the hash down rather than the payload.
 */
public record TransferMoneyCommand(IdempotencyKey idempotencyKey, String requestHash, AccountId sourceAccountId,
                                   AccountId destinationAccountId, BigDecimal amount, String currencyCode,
                                   String reference) implements Command { }
