package com.innovatiopr.payments.integration;

import com.innovatiopr.payments.accounts.AccountId;
import com.innovatiopr.payments.accounts.AccountsApi;
import com.innovatiopr.payments.customers.CustomerId;
import com.innovatiopr.payments.customers.CustomersApi;
import com.innovatiopr.payments.payments.PaymentsApi;
import com.innovatiopr.payments.shared.domain.DomainException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The tests that justify pessimistic locking and the unique index on the idempotency key.
 *
 * <p>Both scenarios pass trivially when requests are serialised, so each one starts its threads behind a
 * {@link CountDownLatch} to make them contend for the same rows at the same moment.
 */
class ConcurrentTransferIT extends AbstractIntegrationTest {

    @Autowired
    CustomersApi customers;

    @Autowired
    AccountsApi accounts;

    @Autowired
    PaymentsApi payments;

    private AccountId source;
    private AccountId destination;

    @BeforeEach
    void openAccountsWithExactlyOneHundred() {
        CustomerId customer = customers.register("Grace", "Hopper", "grace@example.com");
        source = accounts.open(customer, "USD");
        destination = accounts.open(customer, "USD");
        payments.deposit(source, new BigDecimal("100.00"), "USD", "Opening deposit");
    }

    private BigDecimal balanceOf(AccountId accountId) {
        return jdbc.sql("SELECT balance FROM accounts WHERE id = :id")
                .param("id", accountId.value())
                .query(BigDecimal.class)
                .single();
    }

    /**
     * One thread's outcome: the transaction id it produced, or the exception that refused it.
     *
     * <p>Needed because a business failure now arrives as a thrown exception rather than as a returned
     * value, and an exception on a pool thread would otherwise surface as an {@code ExecutionException}
     * wrapper that says nothing about which rule refused.
     */
    private record Attempt(UUID transactionId, Throwable failure) {

        boolean isSuccess() {
            return failure == null;
        }

        boolean isFailure() {
            return failure != null;
        }

        String failureCode() {
            return failure instanceof DomainException domainException ? domainException.code() : null;
        }
    }

    /** Runs the callables simultaneously, releasing them all from one latch. */
    private List<Attempt> runTogether(List<Callable<UUID>> tasks) throws Exception {
        CountDownLatch startGun = new CountDownLatch(1);
        try (ExecutorService pool = Executors.newFixedThreadPool(tasks.size())) {
            List<Future<UUID>> futures = tasks.stream()
                    .map(task -> pool.submit(() -> {
                        startGun.await();
                        return task.call();
                    }))
                    .toList();

            startGun.countDown();
            pool.shutdown();
            assertThat(pool.awaitTermination(60, TimeUnit.SECONDS)).isTrue();

            return futures.stream().map(future -> {
                try {
                    return new Attempt(future.get(), null);
                } catch (ExecutionException e) {
                    return new Attempt(null, e.getCause());
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException(e);
                }
            }).toList();
        }
    }

    @Test
    @DisplayName("two concurrent 80.00 transfers from a 100.00 account: exactly one succeeds")
    void prevents_double_spending() throws Exception {
        List<Attempt> results = runTogether(List.of(
                () -> payments.transfer(UUID.randomUUID().toString(), source, destination,
                        new BigDecimal("80.00"), "USD", "First"),
                () -> payments.transfer(UUID.randomUUID().toString(), source, destination,
                        new BigDecimal("80.00"), "USD", "Second")));

        long succeeded = results.stream().filter(Attempt::isSuccess).count();
        long failed = results.stream().filter(Attempt::isFailure).count();

        assertThat(succeeded).as("exactly one transfer may succeed").isEqualTo(1);
        assertThat(failed).isEqualTo(1);
        assertThat(results.stream().filter(Attempt::isFailure).findFirst().orElseThrow().failureCode())
                .isEqualTo("ACCOUNT_INSUFFICIENT_FUNDS");

        // Without SELECT ... FOR UPDATE both threads would read 100.00, both would decide there are
        // sufficient funds, and the account would end at 20.00 having sent 160.00.
        assertThat(balanceOf(source)).isEqualByComparingTo("20.00");
        assertThat(balanceOf(destination)).isEqualByComparingTo("80.00");
        assertThat(balanceOf(source).signum()).isNotNegative();
    }

    @Test
    @DisplayName("only the successful transfer leaves a transaction and ledger postings")
    void the_failed_attempt_leaves_no_records() throws Exception {
        long ledgerBefore = count("ledger_transactions");

        runTogether(List.of(
                () -> payments.transfer(UUID.randomUUID().toString(), source, destination,
                        new BigDecimal("80.00"), "USD", "First"),
                () -> payments.transfer(UUID.randomUUID().toString(), source, destination,
                        new BigDecimal("80.00"), "USD", "Second")));

        assertThat(count("ledger_transactions")).isEqualTo(ledgerBefore + 1);
        assertThat(count("idempotency_records")).isEqualTo(1);

        Long completedTransfers = jdbc.sql(
                        "SELECT count(*) FROM payment_transactions WHERE type = 'TRANSFER' AND status = 'COMPLETED'")
                .query(Long.class).single();
        assertThat(completedTransfers).isEqualTo(1);
    }

    @Test
    @DisplayName("the same idempotency key sent concurrently moves money once and returns one result")
    void concurrent_retries_of_one_key_move_money_once() throws Exception {
        String sharedKey = UUID.randomUUID().toString();
        int attempts = 6;

        List<Callable<UUID>> tasks = java.util.stream.IntStream.range(0, attempts)
                .<Callable<UUID>>mapToObj(i -> () -> payments.transfer(sharedKey, source, destination,
                        new BigDecimal("30.00"), "USD", "Retry storm"))
                .toList();

        List<Attempt> results = runTogether(tasks);

        // Every attempt should be answered, and all with the same transaction id: the winner's result,
        // replayed. The losers hit the unique index, rolled back, and re-read the committed record.
        List<UUID> transactionIds = results.stream()
                .filter(Attempt::isSuccess)
                .map(Attempt::transactionId)
                .distinct()
                .toList();

        assertThat(transactionIds).as("all successful responses describe the same transaction").hasSize(1);
        assertThat(results.stream().filter(Attempt::isSuccess).count()).isEqualTo(attempts);

        // The decisive assertion: money moved once, however many requests arrived.
        assertThat(balanceOf(source)).isEqualByComparingTo("70.00");
        assertThat(balanceOf(destination)).isEqualByComparingTo("30.00");
        assertThat(count("idempotency_records")).isEqualTo(1);

        Long transferCount = jdbc.sql(
                        "SELECT count(*) FROM payment_transactions WHERE type = 'TRANSFER'")
                .query(Long.class).single();
        assertThat(transferCount).isEqualTo(1);
    }

    @Test
    @DisplayName("opposing transfers between the same two accounts do not deadlock")
    void deterministic_lock_ordering_avoids_deadlock() throws Exception {
        payments.deposit(destination, new BigDecimal("100.00"), "USD", "Fund the other side");

        AtomicInteger failures = new AtomicInteger();
        int rounds = 12;

        // A -> B and B -> A interleaved. Without a total order on AccountId these would form lock
        // cycles and PostgreSQL would abort one side with a deadlock error.
        List<Callable<UUID>> tasks = java.util.stream.IntStream.range(0, rounds)
                .<Callable<UUID>>mapToObj(i -> () -> {
                    AccountId from = i % 2 == 0 ? source : destination;
                    AccountId to = i % 2 == 0 ? destination : source;
                    return payments.transfer(UUID.randomUUID().toString(), from, to,
                            new BigDecimal("1.00"), "USD", "Ping pong " + i);
                })
                .toList();

        runTogether(tasks).stream().filter(Attempt::isFailure).forEach(attempt -> failures.incrementAndGet());

        assertThat(failures.get()).as("no transfer should fail; a deadlock would surface here").isZero();

        // Net effect is zero: six each way of the same amount.
        assertThat(balanceOf(source)).isEqualByComparingTo("100.00");
        assertThat(balanceOf(destination)).isEqualByComparingTo("100.00");
    }
}
