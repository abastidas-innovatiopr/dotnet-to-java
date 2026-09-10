package com.innovatiopr.payments.integration;

import com.innovatiopr.payments.PaymentsApplication;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.simple.JdbcClient;

import java.util.List;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * Base class for integration tests.
 *
 * <h2>A real PostgreSQL, never H2</h2>
 * Almost everything this application depends on for correctness is PostgreSQL-specific behaviour that an
 * in-memory database either lacks or fakes differently:
 * <ul>
 *   <li>{@code SELECT ... FOR UPDATE} blocking semantics, which are what stop a double spend</li>
 *   <li>the exact moment a unique-index violation surfaces, which is what makes idempotency work</li>
 *   <li>{@code NUMERIC(19,4)} arithmetic and rounding</li>
 *   <li>{@code num_nonnulls}, hash indexes, and window functions used by the statement query</li>
 *   <li>the Flyway migrations themselves</li>
 * </ul>
 * A green suite against H2 would prove nothing about any of them.
 *
 * <h2>One container for the whole suite</h2>
 * The container is a {@code static} field started once and never stopped: Testcontainers' Ryuk sidecar
 * removes it when the JVM exits. Starting a database per test class would dominate the runtime.
 */
@SpringBootTest(classes = PaymentsApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public abstract class AbstractIntegrationTest {

    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:18-alpine")
            .withDatabaseName("payments")
            .withUsername("payments")
            .withPassword("payments");

    static {
        POSTGRES.start();
    }

    @DynamicPropertySource
    static void datasourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Autowired
    protected JdbcClient jdbc;

    /**
     * Clears business data between tests, in foreign-key order.
     *
     * <h2>Why DELETE and not TRUNCATE</h2>
     * {@code TRUNCATE} is the faster and more obvious choice, and it deadlocks here. When a test method is
     * {@code @Transactional}, Spring opens the test transaction <em>before</em> {@code @BeforeEach} runs,
     * so the {@code TRUNCATE} executes inside it and takes an {@code ACCESS EXCLUSIVE} lock on every table
     * named — a lock that blocks even plain {@code SELECT}s until the test transaction ends.
     *
     * <p>The transfer path then calls {@code TransferReplayReader}, which is
     * {@code @Transactional(propagation = REQUIRES_NEW)} and therefore reads {@code idempotency_records}
     * on a <em>second</em> connection. That read waits for the {@code ACCESS EXCLUSIVE} lock, the lock is
     * held by the first connection, and the first connection is blocked waiting for the second to return.
     * One thread, two connections, and a deadlock PostgreSQL cannot detect or break — the suite simply
     * hangs.
     *
     * <p>{@code DELETE} takes only {@code ROW EXCLUSIVE}, which does not block readers, so the
     * {@code REQUIRES_NEW} read proceeds against its own MVCC snapshot. Slower, and correct.
     */
    @BeforeEach
    void resetDatabase() {
        for (String table : List.of("idempotency_records", "ledger_entries", "ledger_transactions",
                "payment_transactions", "accounts", "customers", "event_publication")) {
            jdbc.sql("DELETE FROM " + table).update();
        }
    }

    protected long count(String table) {
        Long value = jdbc.sql("SELECT count(*) FROM " + table).query(Long.class).single();
        return value == null ? 0 : value;
    }
}
