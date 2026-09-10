package com.innovatiopr.payments.accounts.domain;

import com.innovatiopr.payments.accounts.AccountId;
import com.innovatiopr.payments.customers.CustomerId;
import com.innovatiopr.payments.shared.domain.AggregateRoot;
import com.innovatiopr.payments.shared.domain.Money;
import com.innovatiopr.payments.shared.domain.MoneyError;
import com.innovatiopr.payments.shared.domain.Result;

import java.time.Instant;
import java.util.Currency;
import java.util.Objects;

/**
 * The Account aggregate root — the consistency boundary for a single balance.
 *
 * <h2>Why this is not anemic</h2>
 * There is no {@code setBalance}. Callers express intent ({@code debit}, {@code credit}, {@code freeze})
 * and this class decides whether the intent is legal. A caller therefore cannot produce a negative
 * balance, move money into a frozen account, or mix currencies, no matter how it is wired up. Contrast:
 * <pre>{@code
 * account.setBalance(account.getBalance().subtract(amount)); // every caller re-implements the rules
 * account.debit(amount);                                     // the rules live in one place
 * }</pre>
 *
 * <h2>Invariants enforced here</h2>
 * <ul>
 *   <li>the account must be ACTIVE to move money in either direction</li>
 *   <li>a FROZEN account can neither send nor receive</li>
 *   <li>a CLOSED account is terminal</li>
 *   <li>amounts must be strictly positive</li>
 *   <li>amount currency must equal account currency</li>
 *   <li>the balance may never go negative — a debit larger than the balance is rejected</li>
 *   <li>an account may only be closed once its balance is zero</li>
 * </ul>
 * The non-negative-balance rule is additionally backed by a {@code CHECK} constraint in PostgreSQL, so
 * even a defective code path or a manual {@code UPDATE} cannot corrupt the invariant.
 *
 * <h2>Aggregate boundaries</h2>
 * The customer is referenced by {@link CustomerId}, not by a {@code Customer} object. Two aggregates are
 * never loaded into one object graph: that would blur the transactional boundary and make it tempting to
 * modify both in one operation.
 */
public final class Account extends AggregateRoot<AccountId> {

    private final AccountId id;
    private final CustomerId customerId;
    private final AccountNumber accountNumber;
    private final Currency currency;
    private Money balance;
    private AccountStatus status;
    private final Instant openedAt;
    private final long version;

    private Account(AccountId id, CustomerId customerId, AccountNumber accountNumber, Currency currency,
                    Money balance, AccountStatus status, Instant openedAt, long version) {
        this.id = Objects.requireNonNull(id, "id");
        this.customerId = Objects.requireNonNull(customerId, "customerId");
        this.accountNumber = Objects.requireNonNull(accountNumber, "accountNumber");
        this.currency = Objects.requireNonNull(currency, "currency");
        this.balance = Objects.requireNonNull(balance, "balance");
        this.status = Objects.requireNonNull(status, "status");
        this.openedAt = Objects.requireNonNull(openedAt, "openedAt");
        this.version = version;
    }

    /**
     * Opens a new account with a zero balance.
     *
     * <h2>Why there is no initial-deposit parameter</h2>
     * An earlier version of this method accepted an opening balance. It produced an account whose balance
     * no ledger posting explained: the money appeared by fiat, so the sum of an account's ledger entries
     * no longer equalled its balance and a statement could not be reconciled against it. For a system
     * whose entire purpose is an auditable ledger, that is a defect rather than a convenience.
     *
     * <p>The fix is to separate two genuinely different events. Opening an account is an Accounts concern
     * and yields an empty account. Funding it is a financial operation — a deposit — owned by the Payments
     * module, which writes the balanced ledger postings and the payment transaction alongside the balance
     * change. Real institutions treat account opening and initial funding as distinct events for exactly
     * this reason.
     *
     * <p>It also keeps the module graph acyclic: posting to the ledger from here would require
     * {@code accounts → ledger}, and Ledger already depends on Accounts for {@code AccountId}.
     */
    public static Result<Account> open(AccountId id, CustomerId customerId, AccountNumber accountNumber,
                                       Currency currency, Instant now) {
        Account account = new Account(id, customerId, accountNumber, currency, Money.zero(currency),
                AccountStatus.ACTIVE, now, 0L);
        account.raise(AccountEvents.opened(id, customerId.toString(), accountNumber, currency, now));
        return Result.success(account);
    }

    /**
     * Rebuilds an aggregate from stored state. No validation, no events: these facts already happened.
     * Called only by {@code AccountPersistenceMapper}.
     */
    public static Account reconstitute(AccountId id, CustomerId customerId, AccountNumber accountNumber,
                                       Currency currency, Money balance, AccountStatus status,
                                       Instant openedAt, long version) {
        return new Account(id, customerId, accountNumber, currency, balance, status, openedAt, version);
    }

    /** Customer-initiated cash-in. */
    public Result<Void> deposit(Money amount, Instant now) {
        Result<Void> check = validateMovement(amount);
        if (check.isFailure()) {
            return check;
        }
        balance = balance.add(amount);
        raise(AccountEvents.deposited(id, amount, balance, now));
        return Result.ok();
    }

    /** Customer-initiated cash-out. */
    public Result<Void> withdraw(Money amount, Instant now) {
        Result<Void> check = validateMovement(amount);
        if (check.isFailure()) {
            return check;
        }
        if (balance.lessThan(amount)) {
            return Result.failure(AccountError.insufficientFunds(id, amount, balance));
        }
        balance = balance.subtract(amount);
        raise(AccountEvents.withdrawn(id, amount, balance, now));
        return Result.ok();
    }

    /** Removes money as one leg of an internal transfer. */
    public Result<Void> debit(Money amount, Instant now) {
        Result<Void> check = validateMovement(amount);
        if (check.isFailure()) {
            return check;
        }
        if (balance.lessThan(amount)) {
            return Result.failure(AccountError.insufficientFunds(id, amount, balance));
        }
        balance = balance.subtract(amount);
        raise(AccountEvents.debited(id, amount, balance, now));
        return Result.ok();
    }

    /** Adds money as one leg of an internal transfer. */
    public Result<Void> credit(Money amount, Instant now) {
        Result<Void> check = validateMovement(amount);
        if (check.isFailure()) {
            return check;
        }
        balance = balance.add(amount);
        raise(AccountEvents.credited(id, amount, balance, now));
        return Result.ok();
    }

    public Result<Void> freeze(Instant now) {
        if (status == AccountStatus.FROZEN) {
            return Result.ok();
        }
        if (status.isTerminal()) {
            return Result.failure(new AccountError.InvalidStatusTransition(id, status, AccountStatus.FROZEN));
        }
        status = AccountStatus.FROZEN;
        raise(AccountEvents.frozen(id, now));
        return Result.ok();
    }

    public Result<Void> unfreeze(Instant now) {
        if (status == AccountStatus.ACTIVE) {
            return Result.ok();
        }
        if (status != AccountStatus.FROZEN) {
            return Result.failure(new AccountError.InvalidStatusTransition(id, status, AccountStatus.ACTIVE));
        }
        status = AccountStatus.ACTIVE;
        raise(AccountEvents.unfrozen(id, now));
        return Result.ok();
    }

    public Result<Void> close(Instant now) {
        if (status == AccountStatus.CLOSED) {
            return Result.ok();
        }
        if (!balance.isZero()) {
            return Result.failure(new AccountError.NotEmpty(id, balance));
        }
        status = AccountStatus.CLOSED;
        raise(AccountEvents.closed(id, now));
        return Result.ok();
    }

    /** Shared guard for every balance-changing operation. */
    private Result<Void> validateMovement(Money amount) {
        Objects.requireNonNull(amount, "amount");
        if (!status.canTransact()) {
            return Result.failure(AccountError.notTransactable(id, status));
        }
        if (!amount.currency().equals(currency)) {
            return Result.failure(MoneyError.currencyMismatch(currency, amount.currency()));
        }
        if (!amount.isPositive()) {
            return Result.failure(MoneyError.amountMustBePositive());
        }
        return Result.ok();
    }

    @Override
    public AccountId id() {
        return id;
    }

    public CustomerId customerId() {
        return customerId;
    }

    public AccountNumber accountNumber() {
        return accountNumber;
    }

    public Currency currency() {
        return currency;
    }

    public Money balance() {
        return balance;
    }

    public AccountStatus status() {
        return status;
    }

    public Instant openedAt() {
        return openedAt;
    }

    public long version() {
        return version;
    }
}
