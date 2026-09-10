package com.innovatiopr.payments.accounts.status.application;

import com.innovatiopr.payments.accounts.application.AccountRepository;
import com.innovatiopr.payments.shared.application.CommandHandler;
import com.innovatiopr.payments.shared.application.DomainEventPublisher;
import com.innovatiopr.payments.shared.domain.Result;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;

@Service
@Transactional
public class FreezeAccountHandler
        implements CommandHandler<AccountStatusCommands.FreezeAccountCommand, AccountStatusResult> {

    private final AccountLifecycle lifecycle;

    public FreezeAccountHandler(AccountRepository accounts, DomainEventPublisher events, Clock clock) {
        this.lifecycle = new AccountLifecycle(accounts, events, clock);
    }

    @Override
    public Result<AccountStatusResult> handle(AccountStatusCommands.FreezeAccountCommand command) {
        return lifecycle.apply(command.accountId(), (account, now) -> account.freeze(now));
    }
}
