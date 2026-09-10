package com.innovatiopr.payments.accounts.infrastructure;

import com.innovatiopr.payments.accounts.AccountId;
import com.innovatiopr.payments.accounts.details.application.AccountBalance;
import com.innovatiopr.payments.accounts.details.application.AccountDetails;
import com.innovatiopr.payments.accounts.details.application.AccountReadModel;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * Read side for accounts.
 *
 * <p>{@code GET /accounts/{id}} does not load the {@code Account} aggregate. Rendering a read-only
 * representation through the write model would cost a persistence context, a mapping pass and a
 * managed-entity lifecycle for data nobody is going to change.
 */
@Repository
class JdbcAccountReadModel implements AccountReadModel {

    private final JdbcClient jdbc;

    JdbcAccountReadModel(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public Optional<AccountDetails> findById(AccountId id) {
        return jdbc.sql("""
                        SELECT id, customer_id, account_number, currency, balance, status, opened_at
                        FROM accounts
                        WHERE id = :id
                        """)
                .param("id", id.value())
                .query((rs, rowNum) -> new AccountDetails(
                        rs.getObject("id", UUID.class),
                        rs.getObject("customer_id", UUID.class),
                        rs.getString("account_number"),
                        rs.getString("currency"),
                        rs.getBigDecimal("balance"),
                        rs.getString("status"),
                        rs.getTimestamp("opened_at").toInstant()))
                .optional();
    }

    @Override
    public Optional<AccountBalance> findBalance(AccountId id) {
        return jdbc.sql("SELECT id, currency, balance, status FROM accounts WHERE id = :id")
                .param("id", id.value())
                .query((rs, rowNum) -> new AccountBalance(
                        rs.getObject("id", UUID.class),
                        rs.getString("currency"),
                        rs.getBigDecimal("balance"),
                        rs.getString("status"),
                        Instant.now()))
                .optional();
    }
}
