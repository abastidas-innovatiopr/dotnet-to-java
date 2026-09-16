package com.innovatiopr.payments.ledger;

import com.innovatiopr.payments.accounts.AccountId;
import com.innovatiopr.payments.shared.domain.DomainException;
import com.innovatiopr.payments.shared.domain.Money;

/**
 * The Ledger module's published contract.
 *
 * <h2>Why these calls are synchronous</h2>
 * Every method here is invoked <em>inside</em> the caller's transaction, not through an event. A transfer
 * that moved balances but failed to write its ledger postings would leave the books wrong, and no amount
 * of retrying an asynchronous handler fixes an inconsistency a client has already been told succeeded.
 * Balances, the payment transaction, the ledger postings and the idempotency record commit together or
 * not at all.
 *
 * <p>Events are still published — but they describe what <em>did</em> happen, after the commit. They are
 * not the mechanism by which it happens.
 *
 * <p>That is also why a failure here throws rather than returning: the exception rolls back the balance
 * changes the caller already staged, which is exactly the outcome the paragraph above demands.
 *
 * @throws DomainException when the requested postings would not balance
 */
public interface LedgerApi {

    LedgerTransactionId recordTransfer(PostingReference reference, AccountId source,
                                       AccountId destination, Money amount, String description);

    LedgerTransactionId recordDeposit(PostingReference reference, AccountId account,
                                      Money amount, String description);

    LedgerTransactionId recordWithdrawal(PostingReference reference, AccountId account,
                                         Money amount, String description);
}
