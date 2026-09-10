package com.innovatiopr.payments.ledger.domain;

import com.innovatiopr.payments.accounts.AccountId;
import com.innovatiopr.payments.ledger.PostingReference;
import com.innovatiopr.payments.shared.domain.Money;
import com.innovatiopr.payments.shared.domain.Result;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Currency;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * The ledger's defining invariant, tested at the domain level. The same property is asserted against a
 * real database in {@code LedgerInvariantIT}: a rule this important is worth checking on both sides.
 */
class LedgerTransactionTest {

    private static final Currency USD = Currency.getInstance("USD");
    private static final Instant NOW = Instant.parse("2026-01-15T10:00:00Z");

    private static PostingReference reference() {
        return PostingReference.of(UUID.randomUUID());
    }

    private static Money usd(String amount) {
        return Money.of(amount, USD);
    }

    @Test
    void a_transfer_produces_one_debit_and_one_credit_that_balance() {
        AccountId source = AccountId.generate();
        AccountId destination = AccountId.generate();

        LedgerTransaction ledger = LedgerTransaction
                .recordTransfer(reference(), source, destination, usd("100.00"), "Rent", NOW)
                .orElseThrow();

        assertThat(ledger.entries()).hasSize(2);
        assertThat(ledger.totalDebits()).isEqualTo(usd("100.00"));
        assertThat(ledger.totalCredits()).isEqualTo(usd("100.00"));
        assertThat(ledger.isBalanced()).isTrue();

        LedgerEntry debit = ledger.entries().stream().filter(LedgerEntry::isDebit).findFirst().orElseThrow();
        LedgerEntry credit = ledger.entries().stream().filter(LedgerEntry::isCredit).findFirst().orElseThrow();
        assertThat(debit.account().internalAccountId()).contains(source);
        assertThat(credit.account().internalAccountId()).contains(destination);
    }

    @Test
    void a_deposit_balances_the_customer_credit_against_the_settlement_position() {
        AccountId account = AccountId.generate();

        LedgerTransaction ledger = LedgerTransaction
                .recordDeposit(reference(), account, usd("250.00"), "Opening deposit", NOW)
                .orElseThrow();

        assertThat(ledger.isBalanced()).isTrue();

        LedgerEntry credit = ledger.entries().stream().filter(LedgerEntry::isCredit).findFirst().orElseThrow();
        LedgerEntry debit = ledger.entries().stream().filter(LedgerEntry::isDebit).findFirst().orElseThrow();
        assertThat(credit.account().internalAccountId()).contains(account);
        assertThat(debit.account().externalName()).contains(LedgerAccountRef.EXTERNAL_SETTLEMENT);
    }

    @Test
    void a_withdrawal_reverses_the_deposit_legs() {
        AccountId account = AccountId.generate();

        LedgerTransaction ledger = LedgerTransaction
                .recordWithdrawal(reference(), account, usd("40.00"), "ATM", NOW)
                .orElseThrow();

        LedgerEntry debit = ledger.entries().stream().filter(LedgerEntry::isDebit).findFirst().orElseThrow();
        LedgerEntry credit = ledger.entries().stream().filter(LedgerEntry::isCredit).findFirst().orElseThrow();
        assertThat(debit.account().internalAccountId()).contains(account);
        assertThat(credit.account().externalName()).contains(LedgerAccountRef.EXTERNAL_SETTLEMENT);
        assertThat(ledger.isBalanced()).isTrue();
    }

    @Test
    void recording_a_transfer_raises_LedgerTransactionRecorded() {
        LedgerTransaction ledger = LedgerTransaction
                .recordTransfer(reference(), AccountId.generate(), AccountId.generate(), usd("10.00"), "x", NOW)
                .orElseThrow();

        assertThat(ledger.domainEvents()).singleElement()
                .isInstanceOf(LedgerTransactionRecorded.class);
    }

    @Test
    void an_entry_amount_must_be_positive_because_direction_carries_the_sign() {
        assertThatThrownBy(() -> LedgerEntry.debit(
                LedgerAccountRef.internal(AccountId.generate()), usd("-5.00")))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void entries_must_share_one_currency() {
        // Reached through reconstitution rather than a factory, since every factory is single-currency
        // by construction - which is itself the point.
        LedgerTransaction mixed = LedgerTransaction.reconstitute(
                com.innovatiopr.payments.ledger.LedgerTransactionId.generate(),
                reference(),
                java.util.List.of(
                        LedgerEntry.debit(LedgerAccountRef.internal(AccountId.generate()), usd("10.00")),
                        LedgerEntry.credit(LedgerAccountRef.internal(AccountId.generate()),
                                Money.of("10.00", Currency.getInstance("EUR")))),
                "mixed", NOW);

        assertThatThrownBy(mixed::totalCredits)
                .isInstanceOf(com.innovatiopr.payments.shared.domain.CurrencyMismatchException.class);
    }

    @Test
    void the_ledger_never_produces_an_unbalanced_transaction() {
        // Every public factory funnels through one validation, so there is no way to construct an
        // unbalanced LedgerTransaction from application code at all.
        for (String amount : new String[]{"0.01", "1.00", "999999.99"}) {
            Result<LedgerTransaction> result = LedgerTransaction.recordTransfer(
                    reference(), AccountId.generate(), AccountId.generate(), usd(amount), "x", NOW);
            assertThat(result.isSuccess()).isTrue();
            assertThat(result.orElseThrow().isBalanced()).isTrue();
        }
    }
}
