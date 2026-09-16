package com.innovatiopr.payments.accounts;

import com.innovatiopr.payments.customers.CustomerId;
import com.innovatiopr.payments.shared.domain.DomainException;
import com.innovatiopr.payments.shared.domain.Money;
import com.innovatiopr.payments.shared.domain.NotFoundException;

/**
 * The Accounts module's published contract.
 *
 * <p>Notice what is <em>not</em> here: no {@code Account}, no {@code setBalance}, no repository. Other
 * modules may ask for a balance movement and the Accounts module decides whether its invariants allow it.
 * Payments cannot subtract from a balance itself even if it wanted to.
 *
 * <p>The posting methods must be called inside an existing transaction — they are annotated
 * {@code @Transactional(propagation = MANDATORY)} in the implementation, so calling one outside a
 * transaction fails fast instead of silently committing one leg of a transfer on its own.
 *
 * <h2>Failures abort the caller's transaction</h2>
 * Because the implementation participates in the caller's transaction, an exception thrown here marks
 * that <em>shared</em> transaction rollback-only. A caller must therefore not catch one of these and
 * carry on: the commit would fail with {@code UnexpectedRollbackException}. Let it propagate.
 */
public interface AccountsApi {

    boolean exists(AccountId accountId);

    /**
     * Returns normally when the account exists, and otherwise throws this module's own "not found" error.
     *
     * <p>Callers therefore never import {@code AccountErrors}: they ask the question and let the answer
     * propagate. Deciding what counts as an account problem stays with the module that owns accounts.
     *
     * @throws NotFoundException when no account has that id
     */
    void requireExists(AccountId accountId);

    /** Opens a new, empty account. Funding it is a separate deposit — see {@code Account.open}. */
    AccountId open(CustomerId customerId, String currencyCode);

    /**
     * Moves money between two accounts, taking row locks in a deterministic order.
     * Enforces every {@code Account} invariant on both legs.
     *
     * @throws NotFoundException when either account is missing
     * @throws DomainException when an account is not transactable or the source cannot cover the amount
     */
    TransferPostings postTransfer(AccountId source, AccountId destination, Money amount);

    AccountPosting postDeposit(AccountId accountId, Money amount);

    AccountPosting postWithdrawal(AccountId accountId, Money amount);
}
