package com.innovatiopr.payments.accounts.domain;

import com.innovatiopr.payments.accounts.AccountId;
import com.innovatiopr.payments.customers.CustomerId;
import com.innovatiopr.payments.shared.domain.DomainEvent;
import com.innovatiopr.payments.shared.domain.DomainException;
import com.innovatiopr.payments.shared.domain.Money;
import com.innovatiopr.payments.shared.domain.ValidationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Currency;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests for the Account aggregate. No Spring, no database — the aggregate is a plain object, which is
 * the whole point of keeping persistence annotations out of it.
 */
class AccountTest {

    private static final Currency USD = Currency.getInstance("USD");
    private static final Currency EUR = Currency.getInstance("EUR");
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-01-15T10:00:00Z"), ZoneOffset.UTC);

    private Account account;

    @BeforeEach
    void openAnAccount() {
        account = newAccount();
        fund(account, "500.00");
        account.clearDomainEvents();
    }

    private static Account newAccount() {
        return Account.open(AccountId.generate(), CustomerId.generate(),
                AccountNumber.fromStorage("123456789012"), USD, CLOCK.instant());
    }

    private static void fund(Account target, String amount) {
        target.deposit(Money.of(amount, USD), CLOCK.instant());
    }

    private static Money usd(String amount) {
        return Money.of(amount, USD);
    }

    @Nested
    @DisplayName("opening")
    class Opening {

        @Test
        void starts_empty_and_active() {
            Account fresh = newAccount();
            assertThat(fresh.balance()).isEqualTo(Money.zero(USD));
            assertThat(fresh.status()).isEqualTo(AccountStatus.ACTIVE);
        }

        @Test
        void records_an_AccountOpened_event() {
            Account fresh = newAccount();
            assertThat(fresh.domainEvents())
                    .singleElement()
                    .isInstanceOf(AccountEvents.AccountOpened.class);
        }
    }

    @Nested
    @DisplayName("deposits")
    class Deposits {

        @Test
        void increase_the_balance_and_record_the_fact() {
            account.deposit(usd("250.00"), CLOCK.instant());

            assertThat(account.balance()).isEqualTo(usd("750.00"));
            assertThat(account.domainEvents()).singleElement()
                    .isInstanceOf(AccountEvents.MoneyDeposited.class);
        }

        @Test
        void reject_a_zero_amount() {
            assertThatThrownBy(() -> account.deposit(Money.zero(USD), CLOCK.instant()))
                    .isInstanceOf(ValidationException.class)
                    .extracting("code").isEqualTo("MONEY_INVALID_AMOUNT");
            assertThat(account.balance()).isEqualTo(usd("500.00"));
        }

        @Test
        void reject_a_foreign_currency() {
            assertThatThrownBy(() -> account.deposit(Money.of("10.00", EUR), CLOCK.instant()))
                    .isInstanceOf(DomainException.class)
                    .extracting("code").isEqualTo("MONEY_CURRENCY_MISMATCH");
        }
    }

    @Nested
    @DisplayName("withdrawals and debits")
    class Withdrawals {

        @Test
        void reduce_the_balance() {
            account.withdraw(usd("200.00"), CLOCK.instant());
            assertThat(account.balance()).isEqualTo(usd("300.00"));
        }

        @Test
        void may_empty_the_account_exactly() {
            account.withdraw(usd("500.00"), CLOCK.instant());
            assertThat(account.balance().isZero()).isTrue();
        }

        @Test
        void are_refused_when_funds_are_insufficient() {
            // A DomainException, not a ValidationException: the request is well formed, the domain
            // simply refuses it in the account's current state. That is the 422-versus-400 line.
            assertThatThrownBy(() -> account.withdraw(usd("500.01"), CLOCK.instant()))
                    .isInstanceOf(DomainException.class)
                    .extracting("code").isEqualTo("ACCOUNT_INSUFFICIENT_FUNDS");
            assertThat(account.balance()).isEqualTo(usd("500.00"));
            assertThat(account.domainEvents()).isEmpty();
        }

        @Test
        void a_rejected_debit_leaves_no_trace() {
            assertThatThrownBy(() -> account.debit(usd("9999.00"), CLOCK.instant()))
                    .isInstanceOf(DomainException.class);
            assertThat(account.balance()).isEqualTo(usd("500.00"));
            assertThat(account.domainEvents()).isEmpty();
        }

        @Test
        void debit_records_MoneyDebited_not_MoneyWithdrawn() {
            // Different facts: a withdrawal is cash leaving the bank, a debit is one leg of a transfer.
            account.debit(usd("100.00"), CLOCK.instant());
            assertThat(account.domainEvents()).singleElement()
                    .isInstanceOf(AccountEvents.MoneyDebited.class);
        }
    }

    @Nested
    @DisplayName("frozen accounts")
    class Frozen {

        @BeforeEach
        void freeze() {
            account.freeze(CLOCK.instant());
            account.clearDomainEvents();
        }

        @Test
        void cannot_send_money() {
            assertThatThrownBy(() -> account.withdraw(usd("1.00"), CLOCK.instant()))
                    .extracting("code").isEqualTo("ACCOUNT_FROZEN");
        }

        @Test
        void cannot_receive_money_either() {
            // A frozen account is not merely "read only for the owner" - it is out of the payment
            // network in both directions, which is why the HATEOAS links drop `deposit` too.
            assertThatThrownBy(() -> account.credit(usd("1.00"), CLOCK.instant()))
                    .extracting("code").isEqualTo("ACCOUNT_FROZEN");
            assertThatThrownBy(() -> account.deposit(usd("1.00"), CLOCK.instant()))
                    .extracting("code").isEqualTo("ACCOUNT_FROZEN");
        }

        @Test
        void can_be_unfrozen() {
            account.unfreeze(CLOCK.instant());
            assertThat(account.status()).isEqualTo(AccountStatus.ACTIVE);
            account.deposit(usd("1.00"), CLOCK.instant());
            assertThat(account.balance()).isEqualTo(usd("501.00"));
        }

        @Test
        void freezing_twice_is_a_no_op_rather_than_an_error() {
            account.freeze(CLOCK.instant());
            assertThat(account.status()).isEqualTo(AccountStatus.FROZEN);
            assertThat(account.domainEvents()).isEmpty();
        }
    }

    @Nested
    @DisplayName("closing")
    class Closing {

        @Test
        void is_refused_while_the_account_holds_money() {
            assertThatThrownBy(() -> account.close(CLOCK.instant()))
                    .extracting("code").isEqualTo("ACCOUNT_NOT_EMPTY");
            assertThat(account.status()).isEqualTo(AccountStatus.ACTIVE);
        }

        @Test
        void succeeds_once_the_balance_is_zero() {
            account.withdraw(usd("500.00"), CLOCK.instant());
            account.close(CLOCK.instant());
            assertThat(account.status()).isEqualTo(AccountStatus.CLOSED);
        }

        @Test
        void a_closed_account_can_do_nothing() {
            account.withdraw(usd("500.00"), CLOCK.instant());
            account.close(CLOCK.instant());

            assertThatThrownBy(() -> account.deposit(usd("1.00"), CLOCK.instant()))
                    .extracting("code").isEqualTo("ACCOUNT_CLOSED");
            assertThatThrownBy(() -> account.credit(usd("1.00"), CLOCK.instant()))
                    .extracting("code").isEqualTo("ACCOUNT_CLOSED");
        }

        @Test
        void closed_is_terminal_and_cannot_be_unfrozen_back_to_active() {
            account.withdraw(usd("500.00"), CLOCK.instant());
            account.close(CLOCK.instant());

            assertThatThrownBy(() -> account.unfreeze(CLOCK.instant()))
                    .extracting("code").isEqualTo("ACCOUNT_INVALID_STATUS_TRANSITION");
            assertThatThrownBy(() -> account.freeze(CLOCK.instant()))
                    .extracting("code").isEqualTo("ACCOUNT_INVALID_STATUS_TRANSITION");
        }
    }

    @Nested
    @DisplayName("domain events")
    class Events {

        @Test
        void carry_identity_timestamp_and_aggregate_id() {
            account.deposit(usd("10.00"), CLOCK.instant());

            DomainEvent event = account.domainEvents().getFirst();
            assertThat(event.eventId()).isNotNull();
            assertThat(event.occurredAt()).isEqualTo(CLOCK.instant());
            assertThat(event.aggregateId()).isEqualTo(account.id().toString());
            assertThat(event.eventType()).isEqualTo("MoneyDeposited");
        }

        @Test
        void accumulate_across_operations() {
            account.deposit(usd("10.00"), CLOCK.instant());
            account.withdraw(usd("5.00"), CLOCK.instant());
            account.freeze(CLOCK.instant());

            assertThat(account.domainEvents()).hasSize(3);
        }

        @Test
        void draining_returns_them_once_and_clears() {
            account.deposit(usd("10.00"), CLOCK.instant());

            List<DomainEvent> first = account.drainDomainEvents();
            List<DomainEvent> second = account.drainDomainEvents();

            assertThat(first).hasSize(1);
            assertThat(second).isEmpty();
        }

        @Test
        void the_exposed_list_is_a_defensive_copy() {
            account.deposit(usd("10.00"), CLOCK.instant());
            List<DomainEvent> events = account.domainEvents();
            account.clearDomainEvents();
            assertThat(events).hasSize(1);
        }
    }
}
