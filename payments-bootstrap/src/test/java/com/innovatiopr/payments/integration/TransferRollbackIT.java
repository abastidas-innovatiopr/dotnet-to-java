package com.innovatiopr.payments.integration;

import com.innovatiopr.payments.accounts.AccountId;
import com.innovatiopr.payments.accounts.AccountsApi;
import com.innovatiopr.payments.customers.CustomerId;
import com.innovatiopr.payments.customers.CustomersApi;
import com.innovatiopr.payments.ledger.LedgerApi;
import com.innovatiopr.payments.payments.PaymentsApi;
import com.innovatiopr.payments.shared.domain.DomainException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * The transfer must be all-or-nothing even when the step that fails comes <em>after</em> the money moved.
 *
 * <h2>Why this test exists</h2>
 * {@code AccountsApi.postTransfer} mutates two managed {@code Account} entities, so by the time the ledger
 * is written both balance changes are already staged and Hibernate will flush them at commit. Under the
 * previous {@code Result<T>} error model a ledger failure <em>returned</em> from a {@code @Transactional}
 * method, and a transactional method that returns normally commits — so the balances were persisted with
 * no ledger rows and no payment transaction to explain them. The balance and the ledger disagreed
 * permanently, in an application whose entire purpose is an auditable ledger.
 *
 * <p>Throwing instead of returning is what fixes it: the exception marks the transaction rollback-only and
 * Spring unwinds every staged write. That behaviour is invisible in the other transfer tests, because they
 * only ever fail at {@code postTransfer} — before anything is staged. This one fails deliberately after.
 */
class TransferRollbackIT extends AbstractIntegrationTest {

    @Autowired
    private PaymentsApi payments;

    @Autowired
    private AccountsApi accounts;

    @Autowired
    private CustomersApi customers;

    /**
     * A full mock rather than a spy. {@code LedgerApiAdapter} is a {@code MANDATORY}-propagation bean, so
     * stubbing a spy would invoke the real method through Spring's transaction proxy while no transaction
     * is open and fail during setup rather than during the test.
     */
    @MockitoBean
    private LedgerApi ledger;

    private AccountId source;
    private AccountId destination;

    @BeforeEach
    void openTwoFundedAccounts() {
        CustomerId customer = customers.register("Ada", "Lovelace",
                "ada+" + UUID.randomUUID() + "@example.com");
        source = accounts.open(customer, "USD");
        destination = accounts.open(customer, "USD");

        // Funded with SQL rather than through payments.deposit, because the ledger is mocked out and a
        // deposit would post to it. This test is about transaction boundaries, not about reconciliation,
        // so an opening balance that no ledger entry explains is acceptable here and nowhere else.
        jdbc.sql("UPDATE accounts SET balance = 1000.00 WHERE id = :id")
                .param("id", source.value())
                .update();
    }

    @Test
    @DisplayName("a ledger failure after the balances moved rolls the money back")
    void a_failure_after_the_money_moved_unwinds_the_balances() {
        long ledgerBefore = count("ledger_transactions");
        long transactionsBefore = count("payment_transactions");

        when(ledger.recordTransfer(any(), any(), any(), any(), any()))
                .thenThrow(new DomainException("LEDGER_UNBALANCED", "Injected ledger failure"));

        assertThatThrownBy(() -> payments.transfer(UUID.randomUUID().toString(), source, destination,
                new BigDecimal("250.00"), "USD", "Doomed"))
                .isInstanceOf(DomainException.class)
                .extracting("code").isEqualTo("LEDGER_UNBALANCED");

        // The decisive assertions: the debit and the credit were both staged before the ledger ran, and
        // both must be gone.
        assertThat(balanceOf(source))
                .as("the source must not have been debited by a transfer that failed")
                .isEqualByComparingTo("1000.00");
        assertThat(balanceOf(destination))
                .as("the destination must not have been credited by a transfer that failed")
                .isEqualByComparingTo("0.00");

        assertThat(count("ledger_transactions")).isEqualTo(ledgerBefore);
        assertThat(count("payment_transactions")).isEqualTo(transactionsBefore);
        assertThat(count("idempotency_records")).isZero();
    }

    private BigDecimal balanceOf(AccountId accountId) {
        return jdbc.sql("SELECT balance FROM accounts WHERE id = :id")
                .param("id", accountId.value())
                .query(BigDecimal.class)
                .single();
    }
}
