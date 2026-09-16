package com.innovatiopr.payments;

import com.innovatiopr.payments.accounts.AccountId;
import com.innovatiopr.payments.accounts.AccountsApi;
import com.innovatiopr.payments.customers.CustomerId;
import com.innovatiopr.payments.customers.CustomersApi;
import com.innovatiopr.payments.payments.PaymentsApi;
import com.innovatiopr.payments.shared.domain.ConflictException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.UUID;

/**
 * Populates a development database with a realistic dataset.
 *
 * <h2>Why this seeds through the module APIs rather than with SQL</h2>
 * Fixture SQL for a financial system has to write consistent balances, payment transactions, ledger
 * postings and totals by hand, and keep all of them consistent as the schema evolves. It is easy to
 * produce a seed where the ledger does not balance or a balance has no postings behind it, and the result
 * is demos and tests running against data the application itself could never have produced.
 *
 * <p>Driving the real use cases makes that impossible by construction: every balance here is the result
 * of an actual {@code Account.credit}, and every ledger transaction was built by
 * {@code LedgerTransaction.recordTransfer}. It also exercises the whole write path on every start, so a
 * broken migration shows up immediately.
 *
 * <h2>Why it uses {@code CustomersApi} / {@code AccountsApi} / {@code PaymentsApi}</h2>
 * An earlier version injected the slice handlers directly and {@code ModularityTest} failed: this class
 * sits in the composition root, and reaching into {@code customers.registration.application} from there
 * is exactly the boundary violation Spring Modulith exists to catch. Rather than exclude this class from
 * verification, each module now publishes the operations it wants callable from outside. The rule stayed
 * strict and the module contracts got better, which is the trade worth making.
 *
 * <p>{@code @Profile("local")} — this never runs in production.
 */
@Component
@Profile("local")
class DevelopmentDataSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DevelopmentDataSeeder.class);

    /** Enough transfers to page through at the default size of 20. */
    private static final int TRANSFER_PAIRS = 41;

    private static final List<String> REFERENCES = List.of("Rent payment", "Groceries", "Utilities",
            "Savings top-up", "Book transfer", "Coffee fund", "Car insurance", "Gym membership");

    private final CustomersApi customers;
    private final AccountsApi accounts;
    private final PaymentsApi payments;

    DevelopmentDataSeeder(CustomersApi customers, AccountsApi accounts, PaymentsApi payments) {
        this.customers = customers;
        this.accounts = accounts;
        this.payments = payments;
    }

    @Override
    public void run(ApplicationArguments args) {
        CustomerId johnDoe = register("John", "Doe", "john.doe@example.com");
        if (johnDoe == null) {
            log.info("Development data already present; skipping seed");
            return;
        }
        register("Jane", "Smith", "jane.smith@example.com");
        register("Carlos", "Rivera", "carlos.rivera@example.com");

        // Accounts open empty. The opening balance arrives as a real deposit, so every cent in the
        // system is explained by a balanced ledger posting and a statement reconciles against the balance.
        AccountId checking = open(johnDoe);
        AccountId savings = open(johnDoe);
        deposit(checking, new BigDecimal("1000.00"), "Opening deposit");
        deposit(savings, new BigDecimal("5000.00"), "Opening deposit");

        // Transfers are issued in cancelling pairs, so the closing balances stay exactly 1,000.00 and
        // 5,000.00 while the history is long enough to demonstrate pagination.
        for (int i = 0; i < TRANSFER_PAIRS; i++) {
            BigDecimal amount = new BigDecimal(10 + (i * 7) % 90).setScale(2, RoundingMode.UNNECESSARY);
            String reference = REFERENCES.get(i % REFERENCES.size());

            transfer(checking, savings, amount, reference);
            transfer(savings, checking, amount, reference + " (returned)");
        }

        log.info("Seeded 3 customers, 2 accounts, 2 opening deposits and {} transfers. "
                + "Explore the API at http://localhost:8080/docs", TRANSFER_PAIRS * 2);
    }

    /**
     * @return the new customer's id, or {@code null} when this email is already registered.
     *
     * <p>A re-run on an existing database is the expected case, so a duplicate email is the one failure
     * this seeder absorbs. Everything else propagates: a seed that half-succeeded and logged a warning
     * would leave a developer debugging a dataset nobody intended.
     */
    private CustomerId register(String firstName, String lastName, String email) {
        try {
            return customers.register(firstName, lastName, email);
        } catch (ConflictException e) {
            if ("CUSTOMER_EMAIL_ALREADY_REGISTERED".equals(e.code())) {
                return null;
            }
            throw e;
        }
    }

    private AccountId open(CustomerId customerId) {
        return accounts.open(customerId, "USD");
    }

    private void deposit(AccountId accountId, BigDecimal amount, String reference) {
        payments.deposit(accountId, amount, "USD", reference);
    }

    private void transfer(AccountId source, AccountId destination, BigDecimal amount, String reference) {
        payments.transfer(UUID.randomUUID().toString(), source, destination, amount, "USD", reference);
    }
}
