package com.innovatiopr.payments.customers.infrastructure;

import com.innovatiopr.payments.customers.CustomerId;
import com.innovatiopr.payments.customers.directory.application.CustomerDetails;
import com.innovatiopr.payments.customers.directory.application.CustomerReadModel;
import com.innovatiopr.payments.customers.directory.application.CustomerSummary;
import com.innovatiopr.payments.shared.application.PageRequest;
import com.innovatiopr.payments.shared.application.PageResult;
import com.innovatiopr.payments.shared.infrastructure.SortColumns;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Read side for customers, built on {@code JdbcClient} — the Spring equivalent of Dapper.
 *
 * <p>{@code accountCount} comes from a correlated subquery. The {@code Customer} aggregate has no such
 * field and should not: it would be a number the aggregate cannot keep correct, since accounts are opened
 * by another module entirely. On the read side it is one line of SQL.
 */
@Repository
class JdbcCustomerReadModel implements CustomerReadModel {

    private static final SortColumns SORTABLE = new SortColumns(
            Map.of("registeredAt", "c.registered_at",
                   "lastName", "c.last_name",
                   "email", "c.email"),
            "c.registered_at",
            "c.id");

    private static final String SELECT_DETAILS = """
            SELECT c.id, c.first_name, c.last_name, c.email, c.registered_at
            FROM customers c
            """;

    private final JdbcClient jdbc;

    JdbcCustomerReadModel(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public Optional<CustomerDetails> findById(CustomerId id) {
        return jdbc.sql(SELECT_DETAILS + " WHERE c.id = :id")
                .param("id", id.value())
                .query((rs, rowNum) -> new CustomerDetails(
                        rs.getObject("id", java.util.UUID.class),
                        rs.getString("first_name"),
                        rs.getString("last_name"),
                        rs.getString("email"),
                        rs.getTimestamp("registered_at").toInstant()))
                .optional();
    }

    @Override
    public PageResult<CustomerSummary> list(PageRequest request) {
        long total = jdbc.sql("SELECT count(*) FROM customers")
                .query(Long.class)
                .single();

        if (total == 0 || request.offset() >= total) {
            return PageResult.of(List.of(), request, total);
        }

        // The ORDER BY fragment comes from SortColumns, never from the query string.
        String sql = """
                SELECT c.id,
                       c.first_name,
                       c.last_name,
                       c.email,
                       c.registered_at,
                       (SELECT count(*) FROM accounts a WHERE a.customer_id = c.id) AS account_count
                FROM customers c
                ORDER BY %s
                LIMIT :limit OFFSET :offset
                """.formatted(SORTABLE.orderByClause(request.sort()));

        List<CustomerSummary> rows = jdbc.sql(sql)
                .param("limit", request.limit())
                .param("offset", request.offset())
                .query((rs, rowNum) -> new CustomerSummary(
                        rs.getObject("id", java.util.UUID.class),
                        rs.getString("first_name") + " " + rs.getString("last_name"),
                        rs.getString("email"),
                        rs.getTimestamp("registered_at").toInstant(),
                        rs.getLong("account_count")))
                .list();

        return PageResult.of(rows, request, total);
    }
}
