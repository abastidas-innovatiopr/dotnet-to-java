package com.innovatiopr.payments.payments.domain;

import com.innovatiopr.payments.accounts.AccountId;
import com.innovatiopr.payments.shared.domain.AggregateRoot;
import com.innovatiopr.payments.shared.domain.Money;
import com.innovatiopr.payments.shared.domain.Result;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * The record of one financial operation — the aggregate root of the Payments context.
 *
 * <p>Separate from {@code Account} on purpose. A transaction spans two accounts, and an aggregate that
 * contained both would make the consistency boundary the pair of accounts rather than each account, which
 * serialises unrelated transfers. Instead this aggregate references accounts by {@link AccountId} and the
 * application layer coordinates the two boundaries inside one database transaction.
 *
 * <p>State transitions are guarded: only PENDING may become COMPLETED or FAILED, and both are terminal.
 * A completed transfer can never revert to pending.
 */
public final class PaymentTransaction extends AggregateRoot<TransactionId> {

    private final TransactionId id;
    private final TransactionType type;
    private final AccountId sourceAccountId;
    private final AccountId destinationAccountId;
    private final Money amount;
    private final TransactionReference reference;
    private TransactionStatus status;
    private final Instant createdAt;
    private Instant completedAt;
    private String failureCode;

    private PaymentTransaction(TransactionId id, TransactionType type, AccountId sourceAccountId,
                               AccountId destinationAccountId, Money amount, TransactionReference reference,
                               TransactionStatus status, Instant createdAt, Instant completedAt,
                               String failureCode) {
        this.id = Objects.requireNonNull(id, "id");
        this.type = Objects.requireNonNull(type, "type");
        this.sourceAccountId = sourceAccountId;
        this.destinationAccountId = destinationAccountId;
        this.amount = Objects.requireNonNull(amount, "amount");
        this.reference = Objects.requireNonNull(reference, "reference");
        this.status = Objects.requireNonNull(status, "status");
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt");
        this.completedAt = completedAt;
        this.failureCode = failureCode;
    }

    /** Starts a transfer in PENDING. Rejects a transfer to the same account before any money moves. */
    public static Result<PaymentTransaction> initiateTransfer(TransactionId id, AccountId source,
                                                              AccountId destination, Money amount,
                                                              TransactionReference reference, Instant now) {
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(destination, "destination");
        if (source.equals(destination)) {
            return Result.failure(TransferError.sameAccount(source));
        }
        if (!amount.isPositive()) {
            return Result.failure(com.innovatiopr.payments.shared.domain.MoneyError.amountMustBePositive());
        }

        PaymentTransaction transaction = new PaymentTransaction(id, TransactionType.TRANSFER, source,
                destination, amount, reference, TransactionStatus.PENDING, now, null, null);
        transaction.raise(new PaymentEvents.TransferInitiated(UUID.randomUUID(), now, id.toString(),
                source.toString(), destination.toString(), amount.amount(), amount.currency().getCurrencyCode()));
        return Result.success(transaction);
    }

    public static Result<PaymentTransaction> initiateDeposit(TransactionId id, AccountId account, Money amount,
                                                             TransactionReference reference, Instant now) {
        if (!amount.isPositive()) {
            return Result.failure(com.innovatiopr.payments.shared.domain.MoneyError.amountMustBePositive());
        }
        return Result.success(new PaymentTransaction(id, TransactionType.DEPOSIT, null, account, amount,
                reference, TransactionStatus.PENDING, now, null, null));
    }

    public static Result<PaymentTransaction> initiateWithdrawal(TransactionId id, AccountId account, Money amount,
                                                                TransactionReference reference, Instant now) {
        if (!amount.isPositive()) {
            return Result.failure(com.innovatiopr.payments.shared.domain.MoneyError.amountMustBePositive());
        }
        return Result.success(new PaymentTransaction(id, TransactionType.WITHDRAWAL, account, null, amount,
                reference, TransactionStatus.PENDING, now, null, null));
    }

    /** Rebuilds from storage: no validation, no events. */
    public static PaymentTransaction reconstitute(TransactionId id, TransactionType type, AccountId source,
                                                  AccountId destination, Money amount,
                                                  TransactionReference reference, TransactionStatus status,
                                                  Instant createdAt, Instant completedAt, String failureCode) {
        return new PaymentTransaction(id, type, source, destination, amount, reference, status, createdAt,
                completedAt, failureCode);
    }

    public Result<Void> complete(Instant now) {
        if (!status.canTransitionTo(TransactionStatus.COMPLETED)) {
            return Result.failure(TransactionError.invalidTransition(id, status, TransactionStatus.COMPLETED));
        }
        status = TransactionStatus.COMPLETED;
        completedAt = now;
        raise(switch (type) {
            case TRANSFER -> new PaymentEvents.TransferCompleted(UUID.randomUUID(), now, id.toString(),
                    String.valueOf(sourceAccountId), String.valueOf(destinationAccountId), amount.amount(),
                    amount.currency().getCurrencyCode(), reference.value());
            case DEPOSIT -> new PaymentEvents.DepositRecorded(UUID.randomUUID(), now, id.toString(),
                    String.valueOf(destinationAccountId), amount.amount(), amount.currency().getCurrencyCode());
            case WITHDRAWAL -> new PaymentEvents.WithdrawalRecorded(UUID.randomUUID(), now, id.toString(),
                    String.valueOf(sourceAccountId), amount.amount(), amount.currency().getCurrencyCode());
        });
        return Result.ok();
    }

    public Result<Void> fail(String failureCode, Instant now) {
        if (!status.canTransitionTo(TransactionStatus.FAILED)) {
            return Result.failure(TransactionError.invalidTransition(id, status, TransactionStatus.FAILED));
        }
        status = TransactionStatus.FAILED;
        this.failureCode = failureCode;
        completedAt = now;
        if (type == TransactionType.TRANSFER) {
            raise(new PaymentEvents.TransferFailed(UUID.randomUUID(), now, id.toString(),
                    String.valueOf(sourceAccountId), String.valueOf(destinationAccountId), amount.amount(),
                    amount.currency().getCurrencyCode(), failureCode));
        }
        return Result.ok();
    }

    @Override
    public TransactionId id() {
        return id;
    }

    public TransactionType type() {
        return type;
    }

    public Optional<AccountId> sourceAccountId() {
        return Optional.ofNullable(sourceAccountId);
    }

    public Optional<AccountId> destinationAccountId() {
        return Optional.ofNullable(destinationAccountId);
    }

    public Money amount() {
        return amount;
    }

    public TransactionReference reference() {
        return reference;
    }

    public TransactionStatus status() {
        return status;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public Optional<Instant> completedAt() {
        return Optional.ofNullable(completedAt);
    }

    public Optional<String> failureCode() {
        return Optional.ofNullable(failureCode);
    }
}
