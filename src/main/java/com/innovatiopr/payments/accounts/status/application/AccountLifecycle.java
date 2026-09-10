package com.innovatiopr.payments.accounts.status.application;

import com.innovatiopr.payments.accounts.AccountId;
import com.innovatiopr.payments.accounts.application.AccountRepository;
import com.innovatiopr.payments.accounts.domain.Account;
import com.innovatiopr.payments.accounts.domain.AccountError;
import com.innovatiopr.payments.shared.application.DomainEventPublisher;
import com.innovatiopr.payments.shared.domain.Result;

import java.time.Clock;
import java.time.Instant;
import java.util.Optional;

/**
 * The load-apply-save-publish sequence shared by the three lifecycle handlers.
 *
 * <p>A plain collaborator rather than a {@code BaseHandler} superclass. Inheritance here would put shared
 * state and a template method between a handler and the thing it does; composition keeps each handler
 * readable on its own and leaves the class hierarchy flat.
 */
final class AccountLifecycle {

    private final AccountRepository accounts;
    private final DomainEventPublisher events;
    private final Clock clock;

    AccountLifecycle(AccountRepository accounts, DomainEventPublisher events, Clock clock) {
        this.accounts = accounts;
        this.events = events;
        this.clock = clock;
    }

    Result<AccountStatusResult> apply(AccountId accountId, StatusTransition transition) {
        Optional<Account> found = accounts.findByIdForUpdate(accountId);
        if (found.isEmpty()) {
            return Result.failure(AccountError.notFound(accountId));
        }
        Account account = found.get();
        Result<Void> applied = transition.apply(account, clock.instant());
        if (applied.isFailure()) {
            return applied.propagate();
        }
        accounts.save(account);
        events.publishFrom(account);
        return Result.success(new AccountStatusResult(account.id().value(), account.status().name()));
    }

    @FunctionalInterface
    interface StatusTransition {
        Result<Void> apply(Account account, Instant now);
    }
}
