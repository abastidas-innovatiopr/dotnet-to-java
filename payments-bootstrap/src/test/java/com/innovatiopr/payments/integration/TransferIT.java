package com.innovatiopr.payments.integration;

import com.innovatiopr.payments.accounts.AccountId;
import com.innovatiopr.payments.accounts.AccountsApi;
import com.innovatiopr.payments.customers.CustomerId;
import com.innovatiopr.payments.customers.CustomersApi;
import com.innovatiopr.payments.payments.PaymentsApi;
import com.innovatiopr.payments.shared.domain.ConflictException;
import com.innovatiopr.payments.shared.domain.DomainException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Transfers against a real PostgreSQL: atomicity, idempotency and the ledger invariant.
 */
class TransferIT extends AbstractIntegrationTest {

    @Autowired
    CustomersApi customers;

    @Autowired
    AccountsApi accounts;

    @Autowired
    PaymentsApi payments;

    private AccountId source;
    private AccountId destination;

    @BeforeEach
    void openFundedAccounts() {
        CustomerId customer = customers.register("Ada", "Lovelace", "ada@example.com");
        source = accounts.open(customer, "USD");
        destination = accounts.open(customer, "USD");
        payments.deposit(source, new BigDecimal("1000.00"), "USD", "Opening deposit");
    }

    private BigDecimal balanceOf(AccountId accountId) {
        return jdbc.sql("SELECT balance FROM accounts WHERE id = :id")
                .param("id", accountId.value())
                .query(BigDecimal.class)
                .single();
    }

    @Test
    @DisplayName("a successful transfer commits balances, transaction, ledger and idempotency together")
    void writes_everything_in_one_transaction() {
        long ledgerBefore = count("ledger_transactions");

        UUID transactionId = payments.transfer(UUID.randomUUID().toString(), source, destination,
                new BigDecimal("250.00"), "USD", "Rent payment");

        assertThat(balanceOf(source)).isEqualByComparingTo("750.00");
        assertThat(balanceOf(destination)).isEqualByComparingTo("250.00");
        assertThat(count("ledger_transactions")).isEqualTo(ledgerBefore + 1);
        assertThat(count("idempotency_records")).isEqualTo(1);

        // Two entries, one on each side, both referencing this transfer.
        Long entries = jdbc.sql("""
                        SELECT count(*) FROM ledger_entries e
                        JOIN ledger_transactions t ON t.id = e.ledger_transaction_id
                        WHERE t.posting_reference = :ref
                        """)
                .param("ref", transactionId)
                .query(Long.class).single();
        assertThat(entries).isEqualTo(2);
    }

    @Test
    @DisplayName("a rejected transfer leaves nothing behind")
    void rolls_everything_back_when_the_domain_refuses() {
        long transactionsBefore = count("payment_transactions");
        long ledgerBefore = count("ledger_transactions");

        assertThatThrownBy(() -> payments.transfer(UUID.randomUUID().toString(), source, destination,
                new BigDecimal("5000.00"), "USD", "Too much"))
                .isInstanceOf(DomainException.class)
                .extracting("code").isEqualTo("ACCOUNT_INSUFFICIENT_FUNDS");

        assertThat(balanceOf(source)).isEqualByComparingTo("1000.00");
        assertThat(balanceOf(destination)).isEqualByComparingTo("0.00");
        assertThat(count("payment_transactions")).isEqualTo(transactionsBefore);
        assertThat(count("ledger_transactions")).isEqualTo(ledgerBefore);
        assertThat(count("idempotency_records")).isZero();
    }

    @Test
    @DisplayName("replaying an idempotency key moves money exactly once")
    void is_idempotent_across_retries() {
        String key = UUID.randomUUID().toString();

        UUID first = payments.transfer(key, source, destination, new BigDecimal("100.00"), "USD", "Rent");
        UUID second = payments.transfer(key, source, destination, new BigDecimal("100.00"), "USD", "Rent");
        UUID third = payments.transfer(key, source, destination, new BigDecimal("100.00"), "USD", "Rent");

        assertThat(first).isEqualTo(second).isEqualTo(third);

        // One transaction, one ledger transaction, one balance change - after three requests.
        assertThat(balanceOf(source)).isEqualByComparingTo("900.00");
        assertThat(balanceOf(destination)).isEqualByComparingTo("100.00");
        assertThat(count("payment_transactions")).isEqualTo(2);   // the opening deposit plus this transfer
        assertThat(count("ledger_transactions")).isEqualTo(2);
        assertThat(count("idempotency_records")).isEqualTo(1);
    }

    @Test
    @DisplayName("reusing a key with a different body is refused")
    void rejects_a_reused_key_carrying_a_different_request() {
        String key = UUID.randomUUID().toString();
        payments.transfer(key, source, destination, new BigDecimal("100.00"), "USD", "Rent");

        // A ConflictException, so the API renders it as 409 — the type is the classification.
        assertThatThrownBy(() -> payments.transfer(key, source, destination,
                new BigDecimal("400.00"), "USD", "Something else"))
                .isInstanceOf(ConflictException.class)
                .extracting("code").isEqualTo("TRANSFER_IDEMPOTENCY_KEY_REUSED");
        assertThat(balanceOf(source)).isEqualByComparingTo("900.00");
    }

    @Test
    @DisplayName("the ledger balances for every completed transaction")
    void every_ledger_transaction_balances() {
        for (int i = 1; i <= 10; i++) {
            payments.transfer(UUID.randomUUID().toString(), source, destination,
                    new BigDecimal(i + ".50"), "USD", "Transfer " + i);
        }

        // Per transaction: debits equal credits.
        Long unbalanced = jdbc.sql("""
                        SELECT count(*) FROM (
                            SELECT e.ledger_transaction_id
                            FROM ledger_entries e
                            GROUP BY e.ledger_transaction_id
                            HAVING SUM(CASE WHEN e.direction = 'DEBIT' THEN e.amount ELSE 0 END)
                                <> SUM(CASE WHEN e.direction = 'CREDIT' THEN e.amount ELSE 0 END)
                        ) unbalanced
                        """)
                .query(Long.class).single();
        assertThat(unbalanced).isZero();

        // System-wide: total debits equal total credits.
        BigDecimal debits = jdbc.sql(
                        "SELECT COALESCE(SUM(amount),0) FROM ledger_entries WHERE direction = 'DEBIT'")
                .query(BigDecimal.class).single();
        BigDecimal credits = jdbc.sql(
                        "SELECT COALESCE(SUM(amount),0) FROM ledger_entries WHERE direction = 'CREDIT'")
                .query(BigDecimal.class).single();
        assertThat(debits).isEqualByComparingTo(credits);
    }

    @Test
    @DisplayName("every account balance is explained by its ledger entries")
    void balances_reconcile_against_the_ledger() {
        payments.transfer(UUID.randomUUID().toString(), source, destination,
                new BigDecimal("321.00"), "USD", "Reconcile me");
        payments.withdraw(destination, new BigDecimal("21.00"), "USD", "Cash out");

        Long mismatched = jdbc.sql("""
                        SELECT count(*) FROM (
                            SELECT a.id
                            FROM accounts a
                            LEFT JOIN ledger_entries e ON e.account_id = a.id
                            GROUP BY a.id, a.balance
                            HAVING a.balance <> COALESCE(SUM(
                                CASE WHEN e.direction = 'CREDIT' THEN e.amount ELSE -e.amount END), 0)
                        ) mismatched
                        """)
                .query(Long.class).single();

        assertThat(mismatched)
                .as("every account balance must equal the sum of its ledger postings")
                .isZero();
    }

    @Test
    @DisplayName("the database refuses a negative balance even if the domain were bypassed")
    void the_check_constraint_backs_the_domain_invariant() {
        org.assertj.core.api.Assertions.assertThatThrownBy(() ->
                        jdbc.sql("UPDATE accounts SET balance = -1 WHERE id = :id")
                                .param("id", source.value())
                                .update())
                .isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("the database refuses an unbalanced ledger transaction")
    void the_check_constraint_backs_the_ledger_invariant() {
        org.assertj.core.api.Assertions.assertThatThrownBy(() ->
                        jdbc.sql("""
                                        INSERT INTO ledger_transactions
                                            (id, posting_reference, description, currency,
                                             total_debits, total_credits, recorded_at)
                                        VALUES (:id, :ref, 'forged', 'USD', 100.0000, 50.0000, now())
                                        """)
                                .param("id", UUID.randomUUID())
                                .param("ref", UUID.randomUUID())
                                .update())
                .isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("domain events reach the publication log and complete")
    void publishes_domain_events_through_the_outbox() {
        payments.transfer(UUID.randomUUID().toString(), source, destination,
                new BigDecimal("10.00"), "USD", "Event test");

        // TransferNotificationListener is async, so give the after-commit dispatch a moment.
        org.awaitility.Awaitility.await()
                .atMost(java.time.Duration.ofSeconds(10))
                .untilAsserted(() -> {
                    Long completed = jdbc.sql(
                                    "SELECT count(*) FROM event_publication WHERE completion_date IS NOT NULL")
                            .query(Long.class).single();
                    assertThat(completed).isPositive();
                });
    }
}
