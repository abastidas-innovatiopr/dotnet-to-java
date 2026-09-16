package com.innovatiopr.payments.accounts.application;

import com.innovatiopr.payments.accounts.AccountId;
import com.innovatiopr.payments.accounts.AccountPosting;
import com.innovatiopr.payments.accounts.TransferPostings;
import com.innovatiopr.payments.accounts.domain.Account;
import com.innovatiopr.payments.accounts.domain.AccountNumber;
import com.innovatiopr.payments.customers.CustomerId;
import com.innovatiopr.payments.shared.application.DomainEventPublisher;
import com.innovatiopr.payments.shared.domain.Money;
import com.innovatiopr.payments.shared.domain.DomainException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Currency;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests the Accounts facade in isolation. The repository is a Mockito mock (the .NET equivalent would be
 * NSubstitute), so these run without a database while still covering the locking protocol.
 */
class AccountsApiAdapterTest {

    private static final Currency USD = Currency.getInstance("USD");
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-01-15T10:00:00Z"), ZoneOffset.UTC);

    private AccountRepository accounts;
    private DomainEventPublisher events;
    private AccountsApiAdapter adapter;

    @BeforeEach
    void setUp() {
        accounts = mock(AccountRepository.class);
        events = mock(DomainEventPublisher.class);
        adapter = new AccountsApiAdapter(accounts, events, CLOCK,
                mock(com.innovatiopr.payments.accounts.opening.application.OpenAccountHandler.class));
    }

    private Account accountWith(AccountId id, String balance) {
        Account account = Account.open(id, CustomerId.generate(), AccountNumber.fromStorage("123456789012"),
                USD, CLOCK.instant());
        if (!balance.equals("0.00")) {
            account.deposit(Money.of(balance, USD), CLOCK.instant());
        }
        account.clearDomainEvents();
        return account;
    }

    private static final AccountId ID_A = AccountId.of(UUID.fromString("00000000-0000-0000-0000-000000000001"));
    private static final AccountId ID_B = AccountId.of(UUID.fromString("ffffffff-ffff-ffff-ffff-ffffffffffff"));

    /**
     * The id that sorts first under {@code AccountId.compareTo}.
     *
     * <p>Derived rather than hard-coded, because {@code UUID.compareTo} compares the two halves as signed
     * longs: {@code ffffffff-...} actually sorts <em>before</em> {@code 00000000-...-0001}. Asserting a
     * guessed order here would test the assumption rather than the behaviour.
     */
    private static AccountId lowerId() {
        return ID_A.compareTo(ID_B) <= 0 ? ID_A : ID_B;
    }

    private static AccountId higherId() {
        return ID_A.compareTo(ID_B) <= 0 ? ID_B : ID_A;
    }

    @Test
    void a_transfer_debits_the_source_and_credits_the_destination() {
        AccountId source = lowerId();
        AccountId destination = higherId();
        when(accounts.findByIdForUpdate(source)).thenReturn(Optional.of(accountWith(source, "100.00")));
        when(accounts.findByIdForUpdate(destination)).thenReturn(Optional.of(accountWith(destination, "10.00")));

        TransferPostings postings = adapter.postTransfer(source, destination, Money.of("40.00", USD));

        assertThat(postings.source().balanceAfter()).isEqualTo(Money.of("60.00", USD));
        assertThat(postings.destination().balanceAfter()).isEqualTo(Money.of("50.00", USD));
        verify(accounts, org.mockito.Mockito.times(2)).save(any(Account.class));
    }

    @Test
    void locks_are_always_taken_in_ascending_id_order_regardless_of_transfer_direction() {
        AccountId low = lowerId();
        AccountId high = higherId();
        when(accounts.findByIdForUpdate(low)).thenReturn(Optional.of(accountWith(low, "100.00")));
        when(accounts.findByIdForUpdate(high)).thenReturn(Optional.of(accountWith(high, "100.00")));

        // high -> low: the transfer runs "backwards", but the locks must not.
        adapter.postTransfer(high, low, Money.of("10.00", USD));

        InOrder order = inOrder(accounts);
        order.verify(accounts).findByIdForUpdate(low);
        order.verify(accounts).findByIdForUpdate(high);

        // This is what prevents a deadlock between concurrent A->B and B->A transfers: both threads
        // request the lower id first, so one waits instead of the two forming a cycle.
    }

    @Test
    void a_missing_source_account_is_reported_and_nothing_is_saved() {
        AccountId source = lowerId();
        AccountId destination = higherId();
        when(accounts.findByIdForUpdate(source)).thenReturn(Optional.empty());
        when(accounts.findByIdForUpdate(destination)).thenReturn(Optional.of(accountWith(destination, "10.00")));

        assertThatThrownBy(() -> adapter.postTransfer(source, destination, Money.of("5.00", USD)))
                .extracting("code").isEqualTo("ACCOUNT_NOT_FOUND");
        verify(accounts, never()).save(any());
        verify(events, never()).publishFrom(any());
    }

    @Test
    void insufficient_funds_abort_the_transfer_before_the_destination_is_credited() {
        AccountId source = lowerId();
        AccountId destination = higherId();
        Account destinationAccount = accountWith(destination, "10.00");
        when(accounts.findByIdForUpdate(source)).thenReturn(Optional.of(accountWith(source, "5.00")));
        when(accounts.findByIdForUpdate(destination)).thenReturn(Optional.of(destinationAccount));

        assertThatThrownBy(() -> adapter.postTransfer(source, destination, Money.of("50.00", USD)))
                .extracting("code").isEqualTo("ACCOUNT_INSUFFICIENT_FUNDS");
        assertThat(destinationAccount.balance()).isEqualTo(Money.of("10.00", USD));
        verify(accounts, never()).save(any());
    }

    @Test
    void a_successful_transfer_publishes_the_events_both_accounts_recorded() {
        AccountId source = lowerId();
        AccountId destination = higherId();
        when(accounts.findByIdForUpdate(source)).thenReturn(Optional.of(accountWith(source, "100.00")));
        when(accounts.findByIdForUpdate(destination)).thenReturn(Optional.of(accountWith(destination, "0.00")));

        adapter.postTransfer(source, destination, Money.of("25.00", USD));

        // publishFrom is varargs, so the captor collects one value per argument.
        ArgumentCaptor<Account> captor = ArgumentCaptor.forClass(Account.class);
        verify(events).publishFrom(captor.capture(), captor.capture());
        assertThat(captor.getAllValues()).hasSize(2);
    }

    @Test
    void a_deposit_credits_a_single_account() {
        AccountId id = lowerId();
        when(accounts.findByIdForUpdate(id)).thenReturn(Optional.of(accountWith(id, "10.00")));

        AccountPosting posting = adapter.postDeposit(id, Money.of("15.00", USD));

        assertThat(posting.balanceAfter()).isEqualTo(Money.of("25.00", USD));
    }

    @Test
    void a_withdrawal_beyond_the_balance_is_refused() {
        AccountId id = lowerId();
        when(accounts.findByIdForUpdate(id)).thenReturn(Optional.of(accountWith(id, "10.00")));

        assertThatThrownBy(() -> adapter.postWithdrawal(id, Money.of("15.00", USD)))
                .extracting("code").isEqualTo("ACCOUNT_INSUFFICIENT_FUNDS");
        verify(accounts, never()).save(any());
    }

    @Test
    void a_transfer_into_a_frozen_account_is_refused() {
        AccountId source = lowerId();
        AccountId destination = higherId();
        Account frozen = accountWith(destination, "0.00");
        frozen.freeze(CLOCK.instant());
        frozen.clearDomainEvents();

        when(accounts.findByIdForUpdate(source)).thenReturn(Optional.of(accountWith(source, "100.00")));
        when(accounts.findByIdForUpdate(destination)).thenReturn(Optional.of(frozen));

        assertThatThrownBy(() -> adapter.postTransfer(source, destination, Money.of("10.00", USD)))
                .extracting("code").isEqualTo("ACCOUNT_FROZEN");
        verify(accounts, never()).save(any());
    }

    @Test
    void exists_delegates_to_the_repository() {
        AccountId id = lowerId();
        when(accounts.existsById(id)).thenReturn(true);
        assertThat(adapter.exists(id)).isTrue();
    }

    @Test
    void a_currency_mismatch_is_an_expected_business_failure_not_a_bug() {
        AccountId source = lowerId();
        AccountId destination = higherId();
        when(accounts.findByIdForUpdate(source)).thenReturn(Optional.of(accountWith(source, "100.00")));
        when(accounts.findByIdForUpdate(destination)).thenReturn(Optional.of(accountWith(destination, "0.00")));

        // A DomainException, not the CurrencyMismatchException that Money's arithmetic guard raises:
        // the caller supplied the wrong currency, which the aggregate expects and refuses cleanly.
        assertThatThrownBy(() -> adapter.postTransfer(source, destination,
                Money.of("10.00", Currency.getInstance("EUR"))))
                .isInstanceOf(DomainException.class)
                .extracting("code").isEqualTo("MONEY_CURRENCY_MISMATCH");
    }

    @Test
    void sorting_account_ids_gives_a_stable_total_order() {
        // Whatever the order is, it must be the same every time and independent of input order - that
        // is the only property deadlock avoidance needs.
        List<AccountId> forwards = List.of(ID_A, ID_B).stream().sorted().toList();
        List<AccountId> backwards = List.of(ID_B, ID_A).stream().sorted().toList();
        assertThat(forwards).isEqualTo(backwards);
        assertThat(forwards).containsExactly(lowerId(), higherId());
    }
}
