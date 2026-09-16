package com.innovatiopr.payments.payments.withdrawals.application;

import com.innovatiopr.payments.accounts.AccountPosting;
import com.innovatiopr.payments.accounts.AccountsApi;
import com.innovatiopr.payments.ledger.LedgerApi;
import com.innovatiopr.payments.ledger.PostingReference;
import com.innovatiopr.payments.payments.application.PaymentTransactionRepository;
import com.innovatiopr.payments.payments.deposits.application.CashMovementResult;
import com.innovatiopr.payments.payments.domain.PaymentTransaction;
import com.innovatiopr.payments.payments.domain.TransactionId;
import com.innovatiopr.payments.payments.domain.TransactionReference;
import com.innovatiopr.payments.shared.application.CommandHandler;
import com.innovatiopr.payments.shared.application.DomainEventPublisher;
import com.innovatiopr.payments.shared.domain.Money;
import com.innovatiopr.payments.shared.domain.MoneyErrors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.Currency;

/** Records a withdrawal: debit the account, credit the settlement position, write the transaction. */
@Service
@Transactional
public class WithdrawMoneyHandler implements CommandHandler<WithdrawMoneyCommand, CashMovementResult> {

    private final AccountsApi accounts;
    private final LedgerApi ledger;
    private final PaymentTransactionRepository transactions;
    private final DomainEventPublisher events;
    private final Clock clock;

    public WithdrawMoneyHandler(AccountsApi accounts, LedgerApi ledger,
                                PaymentTransactionRepository transactions, DomainEventPublisher events,
                                Clock clock) {
        this.accounts = accounts;
        this.ledger = ledger;
        this.transactions = transactions;
        this.events = events;
        this.clock = clock;
    }

    @Override
    public CashMovementResult handle(WithdrawMoneyCommand command) {
        Currency currency;
        try {
            currency = Currency.getInstance(command.currencyCode());
        } catch (IllegalArgumentException | NullPointerException e) {
            throw MoneyErrors.unknownCurrency(String.valueOf(command.currencyCode()));
        }

        Money amount = Money.of(command.amount(), currency);
        TransactionReference reference = TransactionReference.create(command.reference());

        Instant now = clock.instant();
        TransactionId transactionId = TransactionId.generate();
        PaymentTransaction transaction = PaymentTransaction.initiateWithdrawal(transactionId,
                command.accountId(), amount, reference, now);

        // Enforces sufficient funds and account status; takes a row lock for the duration.
        AccountPosting posting = accounts.postWithdrawal(command.accountId(), amount);

        ledger.recordWithdrawal(
                PostingReference.of(transactionId.value()), command.accountId(), amount,
                reference.value());

        transaction.complete(now);
        transactions.save(transaction);
        events.publishFrom(transaction);

        return new CashMovementResult(transactionId.value(), command.accountId().value(),
                amount.amount(), currency.getCurrencyCode(), transaction.status().name(),
                posting.balanceAfter().amount(), now);
    }
}
