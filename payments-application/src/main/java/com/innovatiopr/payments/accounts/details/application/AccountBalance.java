package com.innovatiopr.payments.accounts.details.application;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * The read model's answer to "what is this account worth right now".
 *
 * <p>A projection, not an aggregate: it carries no behaviour and enforces no invariant, because it is
 * assembled by a {@code JdbcClient} query rather than loaded through the write model. Asking the
 * {@code Account} aggregate the same question would mean materialising it, and there is nothing to
 * decide here — only something to report.
 *
 * @param accountId the account this balance belongs to
 * @param currency  ISO-4217 code; an account holds exactly one currency for its whole life
 * @param balance   current balance, at the currency's minor-unit scale
 * @param status    {@code ACTIVE}, {@code FROZEN} or {@code CLOSED} — a closed account still reports a
 *                  balance, because it remains auditable
 * @param asOf      when the balance was observed, which is why the field exists: a balance without a
 *                  timestamp is a claim a client cannot reason about
 */
public record AccountBalance(
        UUID accountId,
        String currency,
        BigDecimal balance,
        String status,
        Instant asOf) { }
