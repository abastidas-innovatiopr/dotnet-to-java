package com.innovatiopr.payments.ledger.statements.application;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * One line of an account statement.
 *
 * @param runningBalance the account's balance immediately after this posting, computed in SQL with a
 *                       window function over the account's whole history. It cannot be derived from the
 *                       page alone — page 3 of a statement still needs the balance carried forward from
 *                       every earlier posting — which is exactly the sort of query the read side exists
 *                       for and the write model should never attempt.
 */
public record StatementLine(UUID entryId, UUID ledgerTransactionId, UUID postingReference, String direction,
                            BigDecimal amount, String currency, BigDecimal runningBalance, String description,
                            Instant recordedAt) { }
