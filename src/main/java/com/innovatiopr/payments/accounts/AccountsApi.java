package com.innovatiopr.payments.accounts;

import com.innovatiopr.payments.shared.domain.Money;
import com.innovatiopr.payments.shared.domain.Result;

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
 */
public interface AccountsApi {

    boolean exists(AccountId accountId);

    /**
     * Succeeds when the account exists, and otherwise fails with this module's own "not found" error.
     *
     * <p>Callers therefore never import {@code AccountError}: they ask the question and propagate the
     * answer. Deciding what counts as an account problem stays with the module that owns accounts.
     */
    Result<Void> requireExists(AccountId accountId);

    /** Opens a new, empty account. Funding it is a separate deposit — see {@code Account.open}. */
    Result<AccountId> open(com.innovatiopr.payments.customers.CustomerId customerId, String currencyCode);

    /**
     * Moves money between two accounts, taking row locks in a deterministic order.
     * Enforces every {@code Account} invariant on both legs.
     */
    Result<TransferPostings> postTransfer(AccountId source, AccountId destination, Money amount);

    Result<AccountPosting> postDeposit(AccountId accountId, Money amount);

    Result<AccountPosting> postWithdrawal(AccountId accountId, Money amount);
}
