package com.innovatiopr.payments.accounts.opening.application;

import com.innovatiopr.payments.accounts.AccountId;
import com.innovatiopr.payments.accounts.application.AccountRepository;
import com.innovatiopr.payments.accounts.domain.Account;
import com.innovatiopr.payments.accounts.domain.AccountNumber;
import com.innovatiopr.payments.customers.CustomersApi;
import com.innovatiopr.payments.customers.domain.CustomerError;
import com.innovatiopr.payments.shared.application.CommandHandler;
import com.innovatiopr.payments.shared.application.DomainEventPublisher;
import com.innovatiopr.payments.shared.domain.Money;
import com.innovatiopr.payments.shared.domain.MoneyError;
import com.innovatiopr.payments.shared.domain.Result;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.util.Currency;

/**
 * Opens an account for an existing customer.
 *
 * <p>Cross-module check: the customer must exist. That question belongs to the Customers module, so it is
 * asked through {@link CustomersApi} rather than by querying the {@code customers} table directly. A
 * Modulith verification test fails the build if any module reaches into another's internals.
 */
@Service
@Transactional
public class OpenAccountHandler implements CommandHandler<OpenAccountCommand, OpenAccountResult> {

    private final AccountRepository accounts;
    private final CustomersApi customers;
    private final DomainEventPublisher events;
    private final Clock clock;

    public OpenAccountHandler(AccountRepository accounts, CustomersApi customers, DomainEventPublisher events,
                              Clock clock) {
        this.accounts = accounts;
        this.customers = customers;
        this.events = events;
        this.clock = clock;
    }

    @Override
    public Result<OpenAccountResult> handle(OpenAccountCommand command) {
        if (!customers.exists(command.customerId())) {
            return Result.failure(CustomerError.notFound(command.customerId()));
        }

        Currency currency;
        try {
            currency = Currency.getInstance(command.currencyCode());
        } catch (IllegalArgumentException | NullPointerException e) {
            return Result.failure(new MoneyError.UnknownCurrency(String.valueOf(command.currencyCode())));
        }

        BigDecimal initial = command.initialDeposit() == null ? BigDecimal.ZERO : command.initialDeposit();
        Money initialDeposit = Money.of(initial, currency);

        AccountNumber accountNumber = generateUnusedAccountNumber();
        Result<Account> opened = Account.open(AccountId.generate(), command.customerId(), accountNumber,
                currency, initialDeposit, clock.instant());
        if (opened.isFailure()) {
            return opened.propagate();
        }

        Account account = opened.orElseThrow();
        accounts.save(account);
        events.publishFrom(account);

        return Result.success(new OpenAccountResult(account.id().value(), account.customerId().value(),
                account.accountNumber().value(), account.currency().getCurrencyCode(),
                account.balance().amount(), account.status().name(), account.openedAt()));
    }

    /**
     * Account numbers are random, so a collision is possible though vanishingly unlikely. Retrying a few
     * times keeps the unique index as the real guarantee while avoiding a spurious failure.
     */
    private AccountNumber generateUnusedAccountNumber() {
        for (int attempt = 0; attempt < 5; attempt++) {
            AccountNumber candidate = AccountNumber.generate();
            if (!accounts.existsByAccountNumber(candidate)) {
                return candidate;
            }
        }
        throw new IllegalStateException("Unable to allocate a free account number after 5 attempts");
    }
}
