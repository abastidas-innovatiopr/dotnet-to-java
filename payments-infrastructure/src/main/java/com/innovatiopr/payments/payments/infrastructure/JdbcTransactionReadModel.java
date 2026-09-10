package com.innovatiopr.payments.payments.infrastructure;

import com.innovatiopr.payments.accounts.AccountId;
import com.innovatiopr.payments.payments.domain.TransactionId;
import com.innovatiopr.payments.payments.transactions.application.TransactionDetails;
import com.innovatiopr.payments.payments.transactions.application.TransactionFilter;
import com.innovatiopr.payments.payments.transactions.application.TransactionReadModel;
import com.innovatiopr.payments.shared.application.PageRequest;
import com.innovatiopr.payments.shared.application.PageResult;
import com.innovatiopr.payments.shared.infrastructure.SortColumns;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Read side for transaction history: filtering, sorting and pagination in one place.
 *
 * <h2>Filters and pagination must agree</h2>
 * The {@code WHERE} clause is built once and used for both the {@code COUNT} and the page query. If the
 * count ignored the filter, a client would be told there are twelve pages and find page three empty. The
 * two statements share {@code buildFilter} precisely so they cannot drift.
 *
 * <p>Every filter value is a bind parameter. Only the {@code ORDER BY} fragment is interpolated, and it
 * comes from {@link SortColumns}, which maps an allow-listed logical name to a fixed column.
 */
@Repository
class JdbcTransactionReadModel implements TransactionReadModel {

    private static final SortColumns SORTABLE = new SortColumns(
            Map.of("createdAt", "created_at",
                   "amount", "amount",
                   "status", "status"),
            "created_at",
            "id");

    private static final String COLUMNS = """
            id, type, source_account_id, destination_account_id, amount, currency,
            status, reference, created_at, completed_at, failure_code
            """;

    private final JdbcClient jdbc;

    JdbcTransactionReadModel(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public Optional<TransactionDetails> findById(TransactionId id) {
        return jdbc.sql("SELECT " + COLUMNS + " FROM payment_transactions WHERE id = :id")
                .param("id", id.value())
                .query(JdbcTransactionReadModel::mapRow)
                .optional();
    }

    @Override
    public PageResult<TransactionDetails> search(TransactionFilter filter, PageRequest page) {
        return query(filter, page, null);
    }

    @Override
    public PageResult<TransactionDetails> findByAccount(AccountId accountId, TransactionFilter filter,
                                                        PageRequest page) {
        return query(filter, page, accountId);
    }

    private PageResult<TransactionDetails> query(TransactionFilter filter, PageRequest page, AccountId accountId) {
        Map<String, Object> params = new HashMap<>();
        String where = buildFilter(filter, accountId, params);

        Long total = jdbc.sql("SELECT count(*) FROM payment_transactions " + where)
                .params(params)
                .query(Long.class)
                .single();

        if (total == null || total == 0 || page.offset() >= total) {
            return PageResult.of(List.of(), page, total == null ? 0 : total);
        }

        String sql = "SELECT " + COLUMNS + " FROM payment_transactions " + where
                + " ORDER BY " + SORTABLE.orderByClause(page.sort())
                + " LIMIT :limit OFFSET :offset";

        params.put("limit", page.limit());
        params.put("offset", page.offset());

        List<TransactionDetails> rows = jdbc.sql(sql)
                .params(params)
                .query(JdbcTransactionReadModel::mapRow)
                .list();

        return PageResult.of(rows, page, total);
    }

    /** Builds a {@code WHERE} clause of bind placeholders only, populating {@code params} as it goes. */
    private String buildFilter(TransactionFilter filter, AccountId accountId, Map<String, Object> params) {
        List<String> clauses = new ArrayList<>();

        if (accountId != null) {
            clauses.add("(source_account_id = :accountId OR destination_account_id = :accountId)");
            params.put("accountId", accountId.value());
        }
        if (filter.type() != null && !filter.type().isBlank()) {
            clauses.add("type = :type");
            params.put("type", filter.type().trim().toUpperCase(java.util.Locale.ROOT));
        }
        if (filter.status() != null && !filter.status().isBlank()) {
            clauses.add("status = :status");
            params.put("status", filter.status().trim().toUpperCase(java.util.Locale.ROOT));
        }
        if (filter.dateFrom() != null) {
            clauses.add("created_at >= :dateFrom");
            params.put("dateFrom", Timestamp.from(filter.dateFrom()));
        }
        if (filter.dateTo() != null) {
            clauses.add("created_at <= :dateTo");
            params.put("dateTo", Timestamp.from(filter.dateTo()));
        }
        if (filter.minimumAmount() != null) {
            clauses.add("amount >= :minimumAmount");
            params.put("minimumAmount", filter.minimumAmount());
        }
        if (filter.maximumAmount() != null) {
            clauses.add("amount <= :maximumAmount");
            params.put("maximumAmount", filter.maximumAmount());
        }

        return clauses.isEmpty() ? "" : "WHERE " + String.join(" AND ", clauses);
    }

    private static TransactionDetails mapRow(ResultSet rs, int rowNum) throws SQLException {
        Timestamp completedAt = rs.getTimestamp("completed_at");
        return new TransactionDetails(
                rs.getObject("id", UUID.class),
                rs.getString("type"),
                rs.getObject("source_account_id", UUID.class),
                rs.getObject("destination_account_id", UUID.class),
                rs.getBigDecimal("amount"),
                rs.getString("currency"),
                rs.getString("status"),
                rs.getString("reference"),
                rs.getTimestamp("created_at").toInstant(),
                completedAt == null ? null : completedAt.toInstant(),
                rs.getString("failure_code"));
    }
}
