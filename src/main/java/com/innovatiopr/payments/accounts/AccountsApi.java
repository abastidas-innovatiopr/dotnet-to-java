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
     * Moves money between two accounts, taking row locks in a deterministic order.
     * Enforces every {@code Account} invariant on both legs.
     */
    Result<TransferPostings> postTransfer(AccountId source, AccountId destination, Money amount);

    Result<AccountPosting> postDeposit(AccountId accountId, Money amount);

    Result<AccountPosting> postWithdrawal(AccountId accountId, Money amount);
}
