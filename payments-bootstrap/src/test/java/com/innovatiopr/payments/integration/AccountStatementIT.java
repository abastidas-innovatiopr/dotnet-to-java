package com.innovatiopr.payments.integration;

import com.innovatiopr.payments.accounts.AccountId;
import com.innovatiopr.payments.accounts.AccountsApi;
import com.innovatiopr.payments.customers.CustomerId;
import com.innovatiopr.payments.customers.CustomersApi;
import com.innovatiopr.payments.ledger.statements.application.GetAccountStatementHandler;
import com.innovatiopr.payments.ledger.statements.application.GetAccountStatementQuery;
import com.innovatiopr.payments.ledger.statements.application.StatementLine;
import com.innovatiopr.payments.payments.PaymentsApi;
import com.innovatiopr.payments.shared.application.PageRequest;
import com.innovatiopr.payments.shared.application.PageResult;
import com.innovatiopr.payments.shared.application.SortSpec;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The account statement read model: a paginated projection whose running balance is computed by a SQL
 * window function over the account's whole history.
 */
class AccountStatementIT extends AbstractIntegrationTest {

    @Autowired
    CustomersApi customers;

    @Autowired
    AccountsApi accounts;

    @Autowired
    PaymentsApi payments;

    @Autowired
    GetAccountStatementHandler statements;

    private AccountId account;
    private AccountId other;

    @BeforeEach
    void buildAHistory() {
        CustomerId customer = customers.register("Tony", "Hoare", "tony@example.com").orElseThrow();
        account = accounts.open(customer, "USD").orElseThrow();
        other = accounts.open(customer, "USD").orElseThrow();

        payments.deposit(account, new BigDecimal("1000.00"), "USD", "Opening deposit").orElseThrow();
        for (int i = 1; i <= 10; i++) {
            payments.transfer(UUID.randomUUID().toString(), account, other,
                    new BigDecimal("10.00"), "USD", "Outgoing " + i).orElseThrow();
        }
        payments.withdraw(account, new BigDecimal("50.00"), "USD", "Cash out").orElseThrow();
    }

    private PageResult<StatementLine> statement(int page, int size, String sortField) {
        return statements.handle(new GetAccountStatementQuery(account, null, null,
                PageRequest.of(page, size, SortSpec.descending(sortField)))).orElseThrow();
    }

    @Test
    @DisplayName("a statement lists one line per posting against the account")
    void lists_every_posting() {
        PageResult<StatementLine> result = statement(0, 50, "recordedAt");

        // 1 deposit + 10 outgoing transfers + 1 withdrawal = 12 postings on this account.
        assertThat(result.totalItems()).isEqualTo(12);
        assertThat(result.items()).hasSize(12);
        assertThat(result.items()).allSatisfy(line -> {
            assertThat(line.currency()).isEqualTo("USD");
            assertThat(line.direction()).isIn("DEBIT", "CREDIT");
            assertThat(line.amount().signum()).isPositive();
        });
    }

    @Test
    @DisplayName("the newest line's running balance equals the account balance")
    void running_balance_reconciles_with_the_account() {
        BigDecimal accountBalance = jdbc.sql("SELECT balance FROM accounts WHERE id = :id")
                .param("id", account.value())
                .query(BigDecimal.class).single();

        StatementLine newest = statement(0, 1, "recordedAt").items().getFirst();

        // 1000 deposited, 10 x 10 transferred out, 50 withdrawn = 850.
        assertThat(accountBalance).isEqualByComparingTo("850.00");
        assertThat(newest.runningBalance()).isEqualByComparingTo(accountBalance);
    }

    @Test
    @DisplayName("the running balance is correct on a later page, not just the first")
    void running_balance_spans_pages() {
        // The point of the window function: page 2's balances depend on every posting before them, which
        // are not in the page being returned. Folding over the page alone would give wrong numbers.
        PageResult<StatementLine> everything = statement(0, 50, "recordedAt");
        PageResult<StatementLine> thirdPage = statement(2, 4, "recordedAt");

        assertThat(thirdPage.items()).hasSize(4);
        for (int i = 0; i < thirdPage.items().size(); i++) {
            StatementLine fromPage = thirdPage.items().get(i);
            StatementLine fromWhole = everything.items().get(8 + i);
            assertThat(fromPage.entryId()).isEqualTo(fromWhole.entryId());
            assertThat(fromPage.runningBalance()).isEqualByComparingTo(fromWhole.runningBalance());
        }
    }

    @Test
    @DisplayName("each line's running balance differs from the previous by that line's signed amount")
    void running_balance_is_internally_consistent() {
        var lines = statement(0, 50, "recordedAt").items();

        // Newest first, so walking backwards reconstructs the arithmetic.
        for (int i = 0; i < lines.size() - 1; i++) {
            StatementLine newer = lines.get(i);
            StatementLine older = lines.get(i + 1);
            BigDecimal delta = "CREDIT".equals(newer.direction())
                    ? newer.amount()
                    : newer.amount().negate();
            assertThat(newer.runningBalance())
                    .isEqualByComparingTo(older.runningBalance().add(delta));
        }
    }

    @Test
    @DisplayName("a statement only shows postings for its own account")
    void is_scoped_to_one_account() {
        PageResult<StatementLine> mine = statement(0, 50, "recordedAt");
        PageResult<StatementLine> theirs = statements.handle(new GetAccountStatementQuery(other, null, null,
                PageRequest.of(0, 50, SortSpec.descending("recordedAt")))).orElseThrow();

        assertThat(mine.totalItems()).isEqualTo(12);
        assertThat(theirs.totalItems()).isEqualTo(10);
        assertThat(mine.items().stream().map(StatementLine::entryId))
                .doesNotContainAnyElementsOf(theirs.items().stream().map(StatementLine::entryId).toList());
    }

    @Test
    @DisplayName("pagination metadata is correct across the whole statement")
    void paginates() {
        assertThat(statement(0, 5, "recordedAt").hasNext()).isTrue();
        assertThat(statement(0, 5, "recordedAt").hasPrevious()).isFalse();
        assertThat(statement(1, 5, "recordedAt").hasNext()).isTrue();
        assertThat(statement(2, 5, "recordedAt").items()).hasSize(2);
        assertThat(statement(2, 5, "recordedAt").hasNext()).isFalse();
        assertThat(statement(9, 5, "recordedAt").items()).isEmpty();
    }

    @Test
    @DisplayName("a date range narrows the statement")
    void filters_by_date() {
        java.time.Instant future = java.time.Instant.now().plusSeconds(3600);

        PageResult<StatementLine> none = statements.handle(new GetAccountStatementQuery(account,
                future, null, PageRequest.of(0, 50, SortSpec.descending("recordedAt")))).orElseThrow();
        assertThat(none.items()).isEmpty();
        assertThat(none.totalItems()).isZero();

        PageResult<StatementLine> all = statements.handle(new GetAccountStatementQuery(account,
                null, future, PageRequest.of(0, 50, SortSpec.descending("recordedAt")))).orElseThrow();
        assertThat(all.totalItems()).isEqualTo(12);
    }

    @Test
    @DisplayName("a statement for an unknown account is a not-found error, not an empty page")
    void unknown_account_is_reported() {
        var result = statements.handle(new GetAccountStatementQuery(AccountId.generate(), null, null,
                PageRequest.of(0, 20, SortSpec.descending("recordedAt"))));

        assertThat(result.isFailure()).isTrue();
        assertThat(result.firstError().code()).isEqualTo("ACCOUNT_NOT_FOUND");
    }

    @Test
    @DisplayName("every statement line links back to the operation that produced it")
    void lines_carry_their_posting_reference() {
        assertThat(statement(0, 50, "recordedAt").items())
                .allSatisfy(line -> {
                    assertThat(line.postingReference()).isNotNull();
                    assertThat(line.ledgerTransactionId()).isNotNull();
                    assertThat(line.description()).isNotBlank();
                });
    }
}
