package com.innovatiopr.payments;

import com.innovatiopr.payments.accounts.AccountId;
import com.innovatiopr.payments.accounts.opening.application.OpenAccountCommand;
import com.innovatiopr.payments.accounts.opening.application.OpenAccountHandler;
import com.innovatiopr.payments.accounts.opening.application.OpenAccountResult;
import com.innovatiopr.payments.customers.CustomerId;
import com.innovatiopr.payments.customers.registration.application.CreateCustomerCommand;
import com.innovatiopr.payments.customers.registration.application.CreateCustomerHandler;
import com.innovatiopr.payments.customers.registration.application.CreateCustomerResult;
import com.innovatiopr.payments.payments.deposits.application.DepositMoneyCommand;
import com.innovatiopr.payments.payments.deposits.application.DepositMoneyHandler;
import com.innovatiopr.payments.payments.domain.IdempotencyKey;
import com.innovatiopr.payments.payments.transfer.application.TransferMoneyCommand;
import com.innovatiopr.payments.payments.transfer.application.TransferMoneyHandler;
import com.innovatiopr.payments.shared.application.RequestHasher;
import com.innovatiopr.payments.shared.domain.Result;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Populates a development database with a realistic dataset.
 *
 * <h2>Why this seeds through the use cases rather than with SQL</h2>
 * Fixture SQL for a financial system has to write consistent balances, payment transactions, ledger
 * postings and totals by hand — and then keep all of them consistent as the schema evolves. It is easy to
 * produce a seed where the ledger does not balance or a balance does not match its postings, and the
 * result is tests and demos that pass against data the application could never have produced.
 *
 * <p>Running the real handlers makes that impossible by construction: every balance here is the result of
 * an actual {@code Account.debit}, and every ledger transaction was built by
 * {@code LedgerTransaction.recordTransfer}. It also exercises the whole write path on every start, which
 * catches a broken migration immediately.
 *
 * <p>{@code @Profile("local")} — this never runs in production.
 */
@Component
@Profile("local")
class DevelopmentDataSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DevelopmentDataSeeder.class);

    /** Enough transfers to demonstrate multiple pages at the default size of 20. */
    private static final int TRANSFER_PAIRS = 41;

    private final CreateCustomerHandler createCustomer;
    private final OpenAccountHandler openAccount;
    private final TransferMoneyHandler transferMoney;
    private final DepositMoneyHandler depositMoney;
    private final JdbcClient jdbc;

    DevelopmentDataSeeder(CreateCustomerHandler createCustomer, OpenAccountHandler openAccount,
                          TransferMoneyHandler transferMoney, DepositMoneyHandler depositMoney,
                          JdbcClient jdbc) {
        this.createCustomer = createCustomer;
        this.openAccount = openAccount;
        this.transferMoney = transferMoney;
        this.depositMoney = depositMoney;
        this.jdbc = jdbc;
    }

    @Override
    public void run(ApplicationArguments args) {
        Long existing = jdbc.sql("SELECT count(*) FROM customers").query(Long.class).single();
        if (existing != null && existing > 0) {
            log.info("Development data already present ({} customers); skipping seed", existing);
            return;
        }

        log.info("Seeding development data...");

        CustomerId johnDoe = register("John", "Doe", "john.doe@example.com");
        register("Jane", "Smith", "jane.smith@example.com");
        register("Carlos", "Rivera", "carlos.rivera@example.com");

        // Accounts open empty; the opening balance arrives as a real deposit so that every cent in the
        // system is explained by a balanced ledger posting.
        AccountId checking = open(johnDoe, "USD");
        AccountId savings = open(johnDoe, "USD");
        deposit(checking, new BigDecimal("1000.00"), "Opening deposit");
        deposit(savings, new BigDecimal("5000.00"), "Opening deposit");

        // Transfers are issued in pairs that cancel out, so the closing balances stay exactly
        // 1,000.00 and 5,000.00 while the history is long enough to page through.
        List<String> references = List.of("Rent payment", "Groceries", "Utilities", "Savings top-up",
                "Book transfer", "Coffee fund", "Car insurance", "Gym membership");

        int completed = 0;
        for (int i = 0; i < TRANSFER_PAIRS; i++) {
            BigDecimal amount = new BigDecimal(10 + (i * 7) % 90).setScale(2, java.math.RoundingMode.UNNECESSARY);
            String reference = references.get(i % references.size());

            if (transfer(checking, savings, amount, reference)) {
                completed++;
            }
            if (transfer(savings, checking, amount, reference + " (returned)")) {
                completed++;
            }
        }

        log.info("Seeded 3 customers, 2 accounts, 2 opening deposits and {} transfers. "
                + "Explore the API at http://localhost:8080/docs", completed);
    }

    private CustomerId register(String firstName, String lastName, String email) {
        Result<CreateCustomerResult> result = createCustomer.handle(
                new CreateCustomerCommand(firstName, lastName, email));
        if (result.isFailure()) {
            throw new IllegalStateException("Seed failed to register " + email + ": " + result.firstError().message());
        }
        return CustomerId.of(result.orElseThrow().customerId());
    }

    private AccountId open(CustomerId customerId, String currency) {
        Result<OpenAccountResult> result = openAccount.handle(new OpenAccountCommand(customerId, currency));
        if (result.isFailure()) {
            throw new IllegalStateException("Seed failed to open account: " + result.firstError().message());
        }
        return AccountId.of(result.orElseThrow().accountId());
    }

    private void deposit(AccountId accountId, BigDecimal amount, String reference) {
        Result<?> result = depositMoney.handle(new DepositMoneyCommand(accountId, amount, "USD", reference));
        if (result.isFailure()) {
            throw new IllegalStateException("Seed failed to deposit: " + result.firstError().message());
        }
    }

    private boolean transfer(AccountId source, AccountId destination, BigDecimal amount, String reference) {
        String canonical = String.join("|", source.toString(), destination.toString(),
                amount.stripTrailingZeros().toPlainString(), "USD", reference);
        Result<?> result = transferMoney.handle(new TransferMoneyCommand(
                IdempotencyKey.fromStorage(UUID.randomUUID().toString()),
                RequestHasher.sha256(canonical),
                source, destination, amount, "USD", reference));

        if (result.isFailure()) {
            log.warn("Seed transfer rejected: {}", result.firstError().message());
            return false;
        }
        return true;
    }
}
