package com.innovatiopr.payments.payments;

import com.innovatiopr.payments.accounts.AccountId;
import com.innovatiopr.payments.shared.domain.ConflictException;
import com.innovatiopr.payments.shared.domain.DomainException;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * The Payments module's published contract: the financial operations it owns.
 *
 * <p>Nothing else in this application consumes it today — Payments is the top of the dependency graph, so
 * no other business module calls into it. It exists because the module's primary use cases are part of
 * its contract, and because the composition root needs a way to drive them without reaching past the
 * module boundary into its internal handlers.
 *
 * <p>That distinction is what Spring Modulith enforces: a module's root package is public and everything
 * beneath it is not. Wanting to call {@code TransferMoneyHandler} from outside is a signal that the
 * module has not finished stating what it offers.
 *
 * <h2>A business failure aborts the caller's transaction</h2>
 * These operations open their own transaction and post through {@code MANDATORY} module adapters, so a
 * refused transfer throws and unwinds every write it had staged. An in-process caller that invokes one
 * of these from inside its own transaction cannot catch the failure and continue — the outer commit
 * would fail with {@code UnexpectedRollbackException}. Drive them from outside a transaction.
 */
public interface PaymentsApi {

    /**
     * @param idempotencyKey client-supplied key making the request safe to retry
     * @return the transaction id
     * @throws ConflictException when the key was already used for a materially different request
     * @throws DomainException when a domain rule refuses the transfer
     */
    UUID transfer(String idempotencyKey, AccountId source, AccountId destination,
                  BigDecimal amount, String currencyCode, String reference);

    UUID deposit(AccountId accountId, BigDecimal amount, String currencyCode, String reference);

    UUID withdraw(AccountId accountId, BigDecimal amount, String currencyCode, String reference);
}
