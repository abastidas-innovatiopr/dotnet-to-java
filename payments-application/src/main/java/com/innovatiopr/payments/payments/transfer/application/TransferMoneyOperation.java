package com.innovatiopr.payments.payments.transfer.application;

import com.innovatiopr.payments.accounts.AccountsApi;
import com.innovatiopr.payments.accounts.TransferPostings;
import com.innovatiopr.payments.ledger.LedgerApi;
import com.innovatiopr.payments.ledger.PostingReference;
import com.innovatiopr.payments.payments.application.IdempotencyStore;
import com.innovatiopr.payments.payments.application.PaymentTransactionRepository;
import com.innovatiopr.payments.payments.domain.IdempotencyRecord;
import com.innovatiopr.payments.payments.domain.PaymentTransaction;
import com.innovatiopr.payments.payments.domain.TransactionId;
import com.innovatiopr.payments.payments.domain.TransactionReference;
import com.innovatiopr.payments.shared.application.DomainEventPublisher;
import com.innovatiopr.payments.shared.domain.Money;
import com.innovatiopr.payments.shared.domain.MoneyError;
import com.innovatiopr.payments.shared.domain.Result;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.Currency;

/**
 * The atomic part of a transfer: everything that must commit together, or not at all.
 *
 * <h2>Why this is a separate bean from {@code TransferMoneyHandler}</h2>
 * Spring implements {@code @Transactional} with a proxy. A caller gets the proxy, the proxy opens the
 * transaction and then delegates to the real object. That means a call from one method of a bean to
 * another method of the <em>same</em> bean ("self-invocation") goes straight to the target and bypasses
 * the proxy entirely — the annotation silently does nothing. It is one of the most common Spring bugs,
 * and it is invisible until something needs to roll back.
 *
 * <p>Here the split is also load-bearing rather than merely stylistic. The handler must be able to catch a
 * duplicate-key failure <em>after</em> this transaction has rolled back and then read the winning record
 * in a fresh transaction. That is only possible if the transaction boundary ends before the catch block —
 * which requires the boundary to be on a different bean. Catching inside the transactional method would
 * leave the handler holding an aborted PostgreSQL transaction on which every further statement fails.
 *
 * <h2>What commits together</h2>
 * source balance, destination balance, payment transaction, ledger postings, idempotency record.
 * Domain events are published to Spring here but their listeners run <em>after</em> commit — see
 * {@code SpringDomainEventPublisher}.
 */
@Service
@Transactional
class TransferMoneyOperation {

    private final AccountsApi accounts;
    private final LedgerApi ledger;
    private final PaymentTransactionRepository transactions;
    private final IdempotencyStore idempotency;
    private final DomainEventPublisher events;
    private final Clock clock;

    TransferMoneyOperation(AccountsApi accounts, LedgerApi ledger, PaymentTransactionRepository transactions,
                           IdempotencyStore idempotency, DomainEventPublisher events, Clock clock) {
        this.accounts = accounts;
        this.ledger = ledger;
        this.transactions = transactions;
        this.idempotency = idempotency;
        this.events = events;
        this.clock = clock;
    }

    Result<TransferMoneyResult> execute(TransferMoneyCommand command) {
        Currency currency;
        try {
            currency = Currency.getInstance(command.currencyCode());
        } catch (IllegalArgumentException | NullPointerException e) {
            return Result.failure(new MoneyError.UnknownCurrency(String.valueOf(command.currencyCode())));
        }

        Money amount = Money.of(command.amount(), currency);
        Result<TransactionReference> reference = TransactionReference.create(command.reference());
        if (reference.isFailure()) {
            return reference.propagate();
        }

        Instant now = clock.instant();
        TransactionId transactionId = TransactionId.generate();

        // The aggregate rejects a same-account transfer before any lock is taken or money moves.
        Result<PaymentTransaction> initiated = PaymentTransaction.initiateTransfer(transactionId,
                command.sourceAccountId(), command.destinationAccountId(), amount, reference.orElseThrow(), now);
        if (initiated.isFailure()) {
            return initiated.propagate();
        }
        PaymentTransaction transaction = initiated.orElseThrow();

        // Locks both accounts in ascending id order and enforces every Account invariant.
        Result<TransferPostings> postings = accounts.postTransfer(command.sourceAccountId(),
                command.destinationAccountId(), amount);
        if (postings.isFailure()) {
            // Deliberately no FAILED row: the whole transaction rolls back, so a rejected transfer leaves
            // no trace beyond the API response. Persisting attempts would need its own transaction and is a
            // separate concern from moving money.
            return postings.propagate();
        }

        Result<com.innovatiopr.payments.ledger.LedgerTransactionId> ledgerResult = ledger.recordTransfer(
                PostingReference.of(transactionId.value()), command.sourceAccountId(),
                command.destinationAccountId(), amount, reference.orElseThrow().value());
        if (ledgerResult.isFailure()) {
            return ledgerResult.propagate();
        }

        Result<Void> completed = transaction.complete(now);
        if (completed.isFailure()) {
            return completed.propagate();
        }
        transactions.save(transaction);

        // Last, and inside the same transaction: the unique index on the key is what makes a retry safe.
        // Throws DuplicateIdempotencyKeyException if a concurrent request claimed the key first, which
        // rolls this whole transaction back — no money moves twice.
        idempotency.save(new IdempotencyRecord(command.idempotencyKey(), command.requestHash(),
                transactionId, 201, now));

        events.publishFrom(transaction);

        TransferPostings applied = postings.orElseThrow();
        return Result.success(new TransferMoneyResult(transactionId.value(), command.sourceAccountId().value(),
                command.destinationAccountId().value(), amount.amount(), currency.getCurrencyCode(),
                transaction.status().name(), reference.orElseThrow().value(),
                applied.source().balanceAfter().amount(), applied.destination().balanceAfter().amount(),
                now, false));
    }
}
