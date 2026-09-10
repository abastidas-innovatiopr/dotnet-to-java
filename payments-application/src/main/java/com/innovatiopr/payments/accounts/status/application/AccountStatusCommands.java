package com.innovatiopr.payments.accounts.status.application;

import com.innovatiopr.payments.accounts.AccountId;
import com.innovatiopr.payments.shared.application.Command;

/** Administrative lifecycle commands for an account. */
public final class AccountStatusCommands {

    private AccountStatusCommands() {
    }

    public record FreezeAccountCommand(AccountId accountId) implements Command { }

    public record UnfreezeAccountCommand(AccountId accountId) implements Command { }

    public record CloseAccountCommand(AccountId accountId) implements Command { }
}
