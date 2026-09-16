package com.innovatiopr.payments.integration;

import com.innovatiopr.payments.accounts.AccountId;
import com.innovatiopr.payments.accounts.AccountsApi;
import com.innovatiopr.payments.accounts.application.AccountRepository;
import com.innovatiopr.payments.accounts.domain.Account;
import com.innovatiopr.payments.accounts.domain.AccountNumber;
import com.innovatiopr.payments.accounts.domain.AccountStatus;
import com.innovatiopr.payments.customers.CustomerId;
import com.innovatiopr.payments.customers.CustomersApi;
import com.innovatiopr.payments.customers.application.CustomerRepository;
import com.innovatiopr.payments.customers.domain.Customer;
import com.innovatiopr.payments.customers.domain.EmailAddress;
import com.innovatiopr.payments.ledger.application.LedgerRepository;
import com.innovatiopr.payments.ledger.domain.LedgerTransaction;
import com.innovatiopr.payments.payments.PaymentsApi;
import com.innovatiopr.payments.shared.domain.Money;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Currency;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Repository behaviour against a real database: the generic base class, the specialised lookups, and
 * round-tripping between aggregates and JPA entities.
 */
class RepositoryIT extends AbstractIntegrationTest {

    private static final Currency USD = Currency.getInstance("USD");

    @Autowired
    AccountRepository accountRepository;

    @Autowired
    CustomerRepository customerRepository;

    @Autowired
    LedgerRepository ledgerRepository;

    @Autowired
    CustomersApi customers;

    @Autowired
    AccountsApi accounts;

    @Autowired
    PaymentsApi payments;

    private CustomerId customer;

    @BeforeEach
    void registerACustomer() {
        customer = customers.register("Barbara", "Liskov", "barbara@example.com");
    }

    @Test
    @DisplayName("an aggregate survives the round trip through JPA unchanged")
    @Transactional
    void maps_an_account_aggregate_both_ways() {
        AccountId id = accounts.open(customer, "USD");
        payments.deposit(id, new BigDecimal("123.45"), "USD", "Deposit");

        Account loaded = accountRepository.findById(id).orElseThrow();

        // Every value object is reconstituted, not just the primitives.
        assertThat(loaded.id()).isEqualTo(id);
        assertThat(loaded.customerId()).isEqualTo(customer);
        assertThat(loaded.currency()).isEqualTo(USD);
        assertThat(loaded.balance()).isEqualTo(Money.of("123.45", USD));
        assertThat(loaded.status()).isEqualTo(AccountStatus.ACTIVE);
        assertThat(loaded.accountNumber().value()).hasSize(12);
        assertThat(loaded.openedAt()).isNotNull();
        assertThat(loaded.domainEvents()).as("reconstitution replays no events").isEmpty();
    }

    @Test
    @DisplayName("findById returns empty for an unknown id rather than throwing")
    @Transactional
    void generic_find_by_id_handles_a_miss() {
        assertThat(accountRepository.findById(AccountId.generate())).isEmpty();
        assertThat(customerRepository.findById(CustomerId.generate())).isEmpty();
    }

    @Test
    @DisplayName("existsById is answered without materialising the aggregate")
    @Transactional
    void generic_exists_by_id() {
        AccountId id = accounts.open(customer, "USD");

        assertThat(accountRepository.existsById(id)).isTrue();
        assertThat(accountRepository.existsById(AccountId.generate())).isFalse();
        assertThat(customerRepository.existsById(customer)).isTrue();
    }

    @Test
    @DisplayName("the specialised lookups the domain actually asked for")
    @Transactional
    void specialised_repository_operations() {
        AccountId id = accounts.open(customer, "USD");
        Account account = accountRepository.findById(id).orElseThrow();
        AccountNumber number = account.accountNumber();

        assertThat(accountRepository.findByAccountNumber(number)).isPresent();
        assertThat(accountRepository.existsByAccountNumber(number)).isTrue();
        assertThat(accountRepository.existsByAccountNumber(AccountNumber.fromStorage("000000000000"))).isFalse();

        // This is the operation no generic CRUD interface would have offered.
        Optional<Account> locked = accountRepository.findByIdForUpdate(id);
        assertThat(locked).isPresent();
        assertThat(locked.orElseThrow().id()).isEqualTo(id);
    }

    @Test
    @DisplayName("customers are found by email through the domain-oriented port")
    @Transactional
    void customer_lookup_by_email() {
        assertThat(customerRepository.existsByEmail(EmailAddress.fromStorage("barbara@example.com"))).isTrue();
        assertThat(customerRepository.existsByEmail(EmailAddress.fromStorage("nobody@example.com"))).isFalse();

        Customer loaded = customerRepository.findById(customer).orElseThrow();
        assertThat(loaded.email().value()).isEqualTo("barbara@example.com");
        assertThat(loaded.name().fullName()).isEqualTo("Barbara Liskov");
    }

    @Test
    @DisplayName("saving an existing aggregate updates rather than inserting")
    @Transactional
    void save_is_an_upsert_on_aggregate_identity() {
        AccountId id = accounts.open(customer, "USD");
        long before = count("accounts");

        Account account = accountRepository.findById(id).orElseThrow();
        account.credit(Money.of("10.00", USD), java.time.Instant.now());
        accountRepository.save(account);

        assertThat(count("accounts")).isEqualTo(before);
    }

    @Test
    @DisplayName("the unique index on account number is enforced by the database")
    void account_number_is_unique() {
        AccountId first = accounts.open(customer, "USD");
        String number = jdbc.sql("SELECT account_number FROM accounts WHERE id = :id")
                .param("id", first.value()).query(String.class).single();

        org.assertj.core.api.Assertions.assertThatThrownBy(() ->
                        jdbc.sql("""
                                        INSERT INTO accounts
                                            (id, customer_id, account_number, currency, balance, status, opened_at, version)
                                        VALUES (:id, :customer, :number, 'USD', 0, 'ACTIVE', now(), 0)
                                        """)
                                .param("id", UUID.randomUUID())
                                .param("customer", customer.value())
                                .param("number", number)
                                .update())
                .isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("a ledger transaction round-trips with all of its entries")
    @Transactional
    void ledger_aggregate_round_trip() {
        AccountId source = accounts.open(customer, "USD");
        AccountId destination = accounts.open(customer, "USD");
        payments.deposit(source, new BigDecimal("100.00"), "USD", "Fund");
        UUID transactionId = payments.transfer(UUID.randomUUID().toString(), source, destination,
                new BigDecimal("30.00"), "USD", "Round trip");

        LedgerTransaction ledger = ledgerRepository
                .findByReference(com.innovatiopr.payments.ledger.PostingReference.of(transactionId))
                .orElseThrow();

        assertThat(ledger.entries()).hasSize(2);
        assertThat(ledger.isBalanced()).isTrue();
        assertThat(ledger.totalDebits()).isEqualTo(Money.of("30.00", USD));
        assertThat(ledger.description()).isEqualTo("Round trip");
    }

    @Test
    @DisplayName("monetary scale survives the database round trip exactly")
    @Transactional
    void money_scale_is_preserved() {
        AccountId id = accounts.open(customer, "USD");
        payments.deposit(id, new BigDecimal("0.01"), "USD", "One cent");

        Account loaded = accountRepository.findById(id).orElseThrow();
        assertThat(loaded.balance()).isEqualTo(Money.of("0.01", USD));
        assertThat(loaded.balance().amount().scale()).isEqualTo(2);
    }
}
