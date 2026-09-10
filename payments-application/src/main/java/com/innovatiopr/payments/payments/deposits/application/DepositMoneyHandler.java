package com.innovatiopr.payments.payments.deposits.application;

import com.innovatiopr.payments.accounts.AccountPosting;
import com.innovatiopr.payments.accounts.AccountsApi;
import com.innovatiopr.payments.ledger.LedgerApi;
import com.innovatiopr.payments.ledger.LedgerTransactionId;
import com.innovatiopr.payments.ledger.PostingReference;
import com.innovatiopr.payments.payments.application.PaymentTransactionRepository;
import com.innovatiopr.payments.payments.domain.PaymentTransaction;
import com.innovatiopr.payments.payments.domain.TransactionId;
import com.innovatiopr.payments.payments.domain.TransactionReference;
import com.innovatiopr.payments.shared.application.CommandHandler;
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
 * Records a deposit: credit the account, debit the settlement position, write the transaction.
 *
 * <h2>Why this lives in Payments and not in Accounts</h2>
 * The URL is {@code POST /api/v1/accounts/{id}/deposits}, but a deposit is a financial operation, and
 * financial operations are the Payments module's job — they produce a {@code PaymentTransaction} and
 * ledger postings. If the Accounts module owned this use case it would need to create payment transactions
 * and ledger entries, so {@code accounts} would import {@code payments} while {@code payments} already
 * imports {@code accounts}. Spring Modulith would fail the build on that cycle, and rightly: neither
 * module could then be understood in isolation.
 *
 * <p>URL shape and module ownership are independent decisions. The resource hierarchy is a statement about
 * the API; the module graph is a statement about the code.
 */
@Service
@Transactional
public class DepositMoneyHandler implements CommandHandler<DepositMoneyCommand, CashMovementResult> {

    private final AccountsApi accounts;
    private final LedgerApi ledger;
    private final PaymentTransactionRepository transactions;
    private final DomainEventPublisher events;
    private final Clock clock;

    public DepositMoneyHandler(AccountsApi accounts, LedgerApi ledger, PaymentTransactionRepository transactions,
                               DomainEventPublisher events, Clock clock) {
        this.accounts = accounts;
        this.ledger = ledger;
        this.transactions = transactions;
        this.events = events;
        this.clock = clock;
    }

    @Override
    public Result<CashMovementResult> handle(DepositMoneyCommand command) {
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
        Result<PaymentTransaction> initiated = PaymentTransaction.initiateDeposit(transactionId,
                command.accountId(), amount, reference.orElseThrow(), now);
        if (initiated.isFailure()) {
            return initiated.propagate();
        }
        PaymentTransaction transaction = initiated.orElseThrow();

        Result<AccountPosting> posting = accounts.postDeposit(command.accountId(), amount);
        if (posting.isFailure()) {
            return posting.propagate();
        }

        Result<LedgerTransactionId> ledgerResult = ledger.recordDeposit(
                PostingReference.of(transactionId.value()), command.accountId(), amount,
                reference.orElseThrow().value());
        if (ledgerResult.isFailure()) {
            return ledgerResult.propagate();
        }

        Result<Void> completed = transaction.complete(now);
        if (completed.isFailure()) {
            return completed.propagate();
        }
        transactions.save(transaction);
        events.publishFrom(transaction);

        return Result.success(new CashMovementResult(transactionId.value(), command.accountId().value(),
                amount.amount(), currency.getCurrencyCode(), transaction.status().name(),
                posting.orElseThrow().balanceAfter().amount(), now));
    }
}
