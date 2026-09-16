package com.innovatiopr.payments.payments.domain;

import com.innovatiopr.payments.accounts.AccountId;
import com.innovatiopr.payments.shared.domain.Money;
import com.innovatiopr.payments.shared.domain.DomainException;
import com.innovatiopr.payments.shared.domain.ValidationException;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Currency;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PaymentTransactionTest {

    private static final Currency USD = Currency.getInstance("USD");
    private static final Instant NOW = Instant.parse("2026-01-15T10:00:00Z");

    private static PaymentTransaction pendingTransfer() {
        return PaymentTransaction.initiateTransfer(TransactionId.generate(), AccountId.generate(),
                AccountId.generate(), Money.of("100.00", USD), TransactionReference.empty(), NOW);
    }

    @Test
    void a_new_transfer_starts_pending_and_records_TransferInitiated() {
        PaymentTransaction transaction = pendingTransfer();

        assertThat(transaction.status()).isEqualTo(TransactionStatus.PENDING);
        assertThat(transaction.completedAt()).isEmpty();
        assertThat(transaction.domainEvents()).singleElement()
                .isInstanceOf(PaymentEvents.TransferInitiated.class);
    }

    @Test
    void a_transfer_to_the_same_account_is_refused_before_anything_happens() {
        AccountId account = AccountId.generate();

        assertThatThrownBy(() -> PaymentTransaction.initiateTransfer(TransactionId.generate(),
                account, account, Money.of("10.00", USD), TransactionReference.empty(), NOW))
                .isInstanceOf(DomainException.class)
                .extracting("code").isEqualTo("TRANSFER_SAME_ACCOUNT");
    }

    @Test
    void a_non_positive_amount_is_refused() {
        assertThatThrownBy(() -> PaymentTransaction.initiateTransfer(TransactionId.generate(),
                AccountId.generate(), AccountId.generate(), Money.zero(USD), TransactionReference.empty(), NOW))
                .isInstanceOf(ValidationException.class)
                .extracting("code").isEqualTo("MONEY_INVALID_AMOUNT");
    }

    @Test
    void completing_a_pending_transfer_records_TransferCompleted() {
        PaymentTransaction transaction = pendingTransfer();
        transaction.clearDomainEvents();

        transaction.complete(NOW);
        assertThat(transaction.status()).isEqualTo(TransactionStatus.COMPLETED);
        assertThat(transaction.completedAt()).contains(NOW);
        assertThat(transaction.domainEvents()).singleElement()
                .isInstanceOf(PaymentEvents.TransferCompleted.class);
    }

    @Test
    void failing_a_pending_transfer_records_TransferFailed_with_the_reason() {
        PaymentTransaction transaction = pendingTransfer();
        transaction.clearDomainEvents();

        transaction.fail("ACCOUNT_INSUFFICIENT_FUNDS", NOW);
        assertThat(transaction.status()).isEqualTo(TransactionStatus.FAILED);
        assertThat(transaction.failureCode()).contains("ACCOUNT_INSUFFICIENT_FUNDS");
        assertThat(transaction.domainEvents()).singleElement()
                .isInstanceOf(PaymentEvents.TransferFailed.class);
    }

    @Test
    void a_completed_transaction_can_never_return_to_pending_or_change_again() {
        PaymentTransaction transaction = pendingTransfer();
        transaction.complete(NOW);
        transaction.clearDomainEvents();

        assertThatThrownBy(() -> transaction.complete(NOW))
                .extracting("code").isEqualTo("TRANSACTION_INVALID_STATE_TRANSITION");
        assertThatThrownBy(() -> transaction.fail("WHATEVER", NOW))
                .extracting("code").isEqualTo("TRANSACTION_INVALID_STATE_TRANSITION");
        assertThat(transaction.status()).isEqualTo(TransactionStatus.COMPLETED);
        assertThat(transaction.domainEvents()).isEmpty();
    }

    @Test
    void a_failed_transaction_is_terminal_too() {
        PaymentTransaction transaction = pendingTransfer();
        transaction.fail("X", NOW);

        assertThatThrownBy(() -> transaction.complete(NOW))
                .isInstanceOf(DomainException.class);
        assertThat(transaction.status()).isEqualTo(TransactionStatus.FAILED);
    }

    @Test
    void a_deposit_has_only_a_destination_leg_and_a_withdrawal_only_a_source() {
        AccountId account = AccountId.generate();

        PaymentTransaction deposit = PaymentTransaction.initiateDeposit(TransactionId.generate(), account,
                Money.of("10.00", USD), TransactionReference.empty(), NOW);
        PaymentTransaction withdrawal = PaymentTransaction.initiateWithdrawal(TransactionId.generate(),
                account, Money.of("10.00", USD), TransactionReference.empty(), NOW);

        assertThat(deposit.sourceAccountId()).isEmpty();
        assertThat(deposit.destinationAccountId()).contains(account);
        assertThat(withdrawal.sourceAccountId()).contains(account);
        assertThat(withdrawal.destinationAccountId()).isEmpty();
    }

    @Test
    void idempotency_keys_are_validated() {
        assertThatThrownBy(() -> IdempotencyKey.create(null))
                .extracting("code").isEqualTo("TRANSFER_IDEMPOTENCY_KEY_REQUIRED");
        assertThatThrownBy(() -> IdempotencyKey.create("short"))
                .extracting("code").isEqualTo("TRANSFER_IDEMPOTENCY_KEY_INVALID");
        assertThat(IdempotencyKey.create("a-valid-key-1234").value()).isEqualTo("a-valid-key-1234");
    }
}
