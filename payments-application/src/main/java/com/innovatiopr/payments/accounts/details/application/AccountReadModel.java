package com.innovatiopr.payments.accounts.details.application;

import com.innovatiopr.payments.accounts.AccountId;

import java.util.Optional;

/** Read-side port for accounts. */
public interface AccountReadModel {

    Optional<AccountDetails> findById(AccountId id);

    Optional<AccountBalance> findBalance(AccountId id);
}
