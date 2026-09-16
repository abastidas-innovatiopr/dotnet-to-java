package com.innovatiopr.payments.accounts.status.application;

import com.innovatiopr.payments.accounts.AccountId;
import com.innovatiopr.payments.accounts.application.AccountRepository;
import com.innovatiopr.payments.accounts.domain.Account;
import com.innovatiopr.payments.accounts.domain.AccountErrors;
import com.innovatiopr.payments.shared.application.DomainEventPublisher;

import java.time.Clock;
import java.time.Instant;

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

    AccountStatusResult apply(AccountId accountId, StatusTransition transition) {
        Account account = accounts.findByIdForUpdate(accountId)
                .orElseThrow(() -> AccountErrors.notFound(accountId));
        transition.apply(account, clock.instant());
        accounts.save(account);
        events.publishFrom(account);
        return new AccountStatusResult(account.id().value(), account.status().name());
    }

    @FunctionalInterface
    interface StatusTransition {
        void apply(Account account, Instant now);
    }
}
