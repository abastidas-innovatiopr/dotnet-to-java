package com.innovatiopr.payments.accounts.application;

import com.innovatiopr.payments.accounts.AccountId;
import com.innovatiopr.payments.accounts.AccountPosting;
import com.innovatiopr.payments.accounts.AccountsApi;
import com.innovatiopr.payments.accounts.TransferPostings;
import com.innovatiopr.payments.accounts.domain.Account;
import com.innovatiopr.payments.accounts.domain.AccountError;
import com.innovatiopr.payments.shared.application.DomainEventPublisher;
import com.innovatiopr.payments.shared.domain.Money;
import com.innovatiopr.payments.shared.domain.Result;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Implements the Accounts module's published contract.
 *
 * <h2>Deterministic lock ordering</h2>
 * {@link #postTransfer} sorts the two account ids and locks them in ascending order, always. Consider two
 * concurrent transfers, A→B and B→A, without that rule:
 * <pre>
 *   thread 1: lock A ... wants B
 *   thread 2: lock B ... wants A     → each holds what the other needs: deadlock
 * </pre>
 * PostgreSQL detects the cycle and kills one transaction with a deadlock error — a 500 for a request that
 * was perfectly valid. With a total order, both threads request A first, so the second simply blocks until
 * the first commits. A cycle cannot form, because a cycle requires at least one thread to acquire locks in
 * descending order. This is why {@code AccountId} implements {@code Comparable}.
 *
 * <h2>Why MANDATORY propagation</h2>
 * These methods must join a transaction the caller already started, never begin one of their own. If
 * {@code postTransfer} opened its own transaction, the debit and credit would commit independently of the
 * payment transaction and ledger rows written by the caller — and a failure afterwards would leave money
 * moved with no record of why. {@code MANDATORY} turns that mistake into an immediate exception instead of
 * a silent inconsistency.
 */
@Service
class AccountsApiAdapter implements AccountsApi {

    private final AccountRepository accounts;
    private final DomainEventPublisher events;
    private final Clock clock;

    AccountsApiAdapter(AccountRepository accounts, DomainEventPublisher events, Clock clock) {
        this.accounts = accounts;
        this.events = events;
        this.clock = clock;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean exists(AccountId accountId) {
        return accounts.existsById(accountId);
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public Result<TransferPostings> postTransfer(AccountId source, AccountId destination, Money amount) {
        Result<List<Account>> locked = lockInDeterministicOrder(List.of(source, destination));
        if (locked.isFailure()) {
            return locked.propagate();
        }

        Account from = locked.orElseThrow().stream().filter(a -> a.id().equals(source)).findFirst().orElseThrow();
        Account to = locked.orElseThrow().stream().filter(a -> a.id().equals(destination)).findFirst().orElseThrow();

        Instant now = clock.instant();
        Result<Void> debit = from.debit(amount, now);
        if (debit.isFailure()) {
            return debit.propagate();
        }
        Result<Void> credit = to.credit(amount, now);
        if (credit.isFailure()) {
            return credit.propagate();
        }

        accounts.save(from);
        accounts.save(to);
        events.publishFrom(from, to);

        return Result.success(new TransferPostings(
                new AccountPosting(from.id(), from.balance()),
                new AccountPosting(to.id(), to.balance())));
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public Result<AccountPosting> postDeposit(AccountId accountId, Money amount) {
        return applySingleLegged(accountId, (account, now) -> account.deposit(amount, now));
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public Result<AccountPosting> postWithdrawal(AccountId accountId, Money amount) {
        return applySingleLegged(accountId, (account, now) -> account.withdraw(amount, now));
    }

    private Result<AccountPosting> applySingleLegged(AccountId accountId, BalanceMovement movement) {
        Optional<Account> found = accounts.findByIdForUpdate(accountId);
        if (found.isEmpty()) {
            return Result.failure(AccountError.notFound(accountId));
        }
        Account account = found.get();
        Result<Void> applied = movement.apply(account, clock.instant());
        if (applied.isFailure()) {
            return applied.propagate();
        }
        accounts.save(account);
        events.publishFrom(account);
        return Result.success(new AccountPosting(account.id(), account.balance()));
    }

    /** Loads every account under a write lock, always in ascending id order. */
    private Result<List<Account>> lockInDeterministicOrder(List<AccountId> ids) {
        List<AccountId> ordered = ids.stream().sorted().toList();
        List<Account> loaded = new ArrayList<>(ordered.size());
        for (AccountId id : ordered) {
            Optional<Account> account = accounts.findByIdForUpdate(id);
            if (account.isEmpty()) {
                return Result.failure(AccountError.notFound(id));
            }
            loaded.add(account.get());
        }
        return Result.success(loaded);
    }

    @FunctionalInterface
    private interface BalanceMovement {
        Result<Void> apply(Account account, Instant now);
    }
}
