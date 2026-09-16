package com.innovatiopr.payments.accounts.application;

import com.innovatiopr.payments.accounts.AccountId;
import com.innovatiopr.payments.accounts.AccountPosting;
import com.innovatiopr.payments.accounts.AccountsApi;
import com.innovatiopr.payments.accounts.TransferPostings;
import com.innovatiopr.payments.accounts.domain.Account;
import com.innovatiopr.payments.accounts.domain.AccountErrors;
import com.innovatiopr.payments.accounts.opening.application.OpenAccountCommand;
import com.innovatiopr.payments.accounts.opening.application.OpenAccountHandler;
import com.innovatiopr.payments.customers.CustomerId;
import com.innovatiopr.payments.shared.application.DomainEventPublisher;
import com.innovatiopr.payments.shared.domain.Money;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

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
 *
 * <p>The same propagation means a failure thrown here marks the <em>caller's</em> transaction
 * rollback-only, which is exactly what a refused transfer should do: the debit is unwound along with
 * everything else the caller had staged. A caller must therefore let it propagate rather than catch it
 * and continue.
 */
@Service
class AccountsApiAdapter implements AccountsApi {

    private final AccountRepository accounts;
    private final DomainEventPublisher events;
    private final Clock clock;
    private final OpenAccountHandler openAccount;

    AccountsApiAdapter(AccountRepository accounts, DomainEventPublisher events, Clock clock,
                       OpenAccountHandler openAccount) {
        this.accounts = accounts;
        this.events = events;
        this.clock = clock;
        this.openAccount = openAccount;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean exists(AccountId accountId) {
        return accounts.existsById(accountId);
    }

    @Override
    @Transactional(readOnly = true)
    public void requireExists(AccountId accountId) {
        if (!accounts.existsById(accountId)) {
            throw AccountErrors.notFound(accountId);
        }
    }

    @Override
    public AccountId open(CustomerId customerId, String currencyCode) {
        return AccountId.of(openAccount.handle(new OpenAccountCommand(customerId, currencyCode)).accountId());
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public TransferPostings postTransfer(AccountId source, AccountId destination, Money amount) {
        List<Account> locked = lockInDeterministicOrder(List.of(source, destination));

        Account from = locked.stream().filter(a -> a.id().equals(source)).findFirst().orElseThrow();
        Account to = locked.stream().filter(a -> a.id().equals(destination)).findFirst().orElseThrow();

        Instant now = clock.instant();
        from.debit(amount, now);
        to.credit(amount, now);

        accounts.save(from);
        accounts.save(to);
        events.publishFrom(from, to);

        return new TransferPostings(
                new AccountPosting(from.id(), from.balance()),
                new AccountPosting(to.id(), to.balance()));
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public AccountPosting postDeposit(AccountId accountId, Money amount) {
        return applySingleLegged(accountId, (account, now) -> account.deposit(amount, now));
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public AccountPosting postWithdrawal(AccountId accountId, Money amount) {
        return applySingleLegged(accountId, (account, now) -> account.withdraw(amount, now));
    }

    private AccountPosting applySingleLegged(AccountId accountId, BalanceMovement movement) {
        Account account = accounts.findByIdForUpdate(accountId)
                .orElseThrow(() -> AccountErrors.notFound(accountId));
        movement.apply(account, clock.instant());
        accounts.save(account);
        events.publishFrom(account);
        return new AccountPosting(account.id(), account.balance());
    }

    /** Loads every account under a write lock, always in ascending id order. */
    private List<Account> lockInDeterministicOrder(List<AccountId> ids) {
        List<AccountId> ordered = ids.stream().sorted().toList();
        List<Account> loaded = new ArrayList<>(ordered.size());
        for (AccountId id : ordered) {
            loaded.add(accounts.findByIdForUpdate(id)
                    .orElseThrow(() -> AccountErrors.notFound(id)));
        }
        return loaded;
    }

    @FunctionalInterface
    private interface BalanceMovement {
        void apply(Account account, Instant now);
    }
}
