package com.innovatiopr.payments.ledger.infrastructure;

import com.innovatiopr.payments.accounts.AccountId;
import com.innovatiopr.payments.ledger.statements.application.StatementLine;
import com.innovatiopr.payments.ledger.statements.application.StatementReadModel;
import com.innovatiopr.payments.shared.application.PageRequest;
import com.innovatiopr.payments.shared.application.PageResult;
import com.innovatiopr.payments.shared.infrastructure.SortColumns;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Read side for account statements — the clearest case for {@code JdbcClient} over JPA in this codebase.
 *
 * <h2>The running balance</h2>
 * Each line shows the balance immediately after that posting. That value cannot be computed from the page
 * being returned: page three of a statement still needs the balance carried forward from every earlier
 * posting. A window function does it in the database:
 *
 * <pre>{@code
 * SUM(signed_amount) OVER (ORDER BY recorded_at, id ROWS UNBOUNDED PRECEDING)
 * }</pre>
 *
 * The alternative through the write model would be to load every ledger entry for the account into memory
 * and fold over them, on every request — which is precisely the read-side query that CQRS exists to let
 * you write differently.
 *
 * <p>Note the ordering asymmetry: the window is computed oldest-first, because a running balance only
 * means anything in chronological order, while the page is returned newest-first, because that is how a
 * statement is read. Both live in the same statement, one nested inside the other.
 */
@Repository
class JdbcStatementReadModel implements StatementReadModel {

    private static final SortColumns SORTABLE = new SortColumns(
            Map.of("recordedAt", "recorded_at", "amount", "amount"),
            "recorded_at",
            "entry_id");

    private final JdbcClient jdbc;

    JdbcStatementReadModel(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public PageResult<StatementLine> findForAccount(AccountId accountId, Instant from, Instant to,
                                                    PageRequest page) {
        Map<String, Object> params = new HashMap<>();
        params.put("accountId", accountId.value());

        StringBuilder dateFilter = new StringBuilder();
        if (from != null) {
            dateFilter.append(" AND e.recorded_at >= :from");
            params.put("from", Timestamp.from(from));
        }
        if (to != null) {
            dateFilter.append(" AND e.recorded_at <= :to");
            params.put("to", Timestamp.from(to));
        }

        Long total = jdbc.sql("""
                        SELECT count(*)
                        FROM ledger_entries e
                        WHERE e.account_id = :accountId
                        """ + dateFilter)
                .params(params)
                .query(Long.class)
                .single();

        if (total == null || total == 0 || page.offset() >= total) {
            return PageResult.of(List.of(), page, total == null ? 0 : total);
        }

        // The running balance is computed over the account's whole history (subject to the date filter),
        // oldest first; the outer query then orders newest first and slices out the requested page.
        String sql = """
                SELECT *
                FROM (
                    SELECT e.id                AS entry_id,
                           e.ledger_transaction_id,
                           t.posting_reference,
                           e.direction,
                           e.amount,
                           e.currency,
                           t.description,
                           e.recorded_at,
                           SUM(CASE WHEN e.direction = 'CREDIT' THEN e.amount ELSE -e.amount END)
                               OVER (ORDER BY e.recorded_at, e.id
                                     ROWS BETWEEN UNBOUNDED PRECEDING AND CURRENT ROW) AS running_balance
                    FROM ledger_entries e
                    JOIN ledger_transactions t ON t.id = e.ledger_transaction_id
                    WHERE e.account_id = :accountId
                    %s
                ) lines
                ORDER BY %s
                LIMIT :limit OFFSET :offset
                """.formatted(dateFilter, SORTABLE.orderByClause(page.sort()));

        params.put("limit", page.limit());
        params.put("offset", page.offset());

        List<StatementLine> rows = jdbc.sql(sql)
                .params(params)
                .query((rs, rowNum) -> new StatementLine(
                        rs.getObject("entry_id", UUID.class),
                        rs.getObject("ledger_transaction_id", UUID.class),
                        rs.getObject("posting_reference", UUID.class),
                        rs.getString("direction"),
                        rs.getBigDecimal("amount"),
                        rs.getString("currency"),
                        rs.getBigDecimal("running_balance"),
                        rs.getString("description"),
                        rs.getTimestamp("recorded_at").toInstant()))
                .list();

        return PageResult.of(rows, page, total);
    }
}
