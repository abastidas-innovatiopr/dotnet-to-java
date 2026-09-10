package com.innovatiopr.payments.ledger.domain;

import com.innovatiopr.payments.shared.domain.DomainError;
import com.innovatiopr.payments.shared.domain.ErrorType;
import com.innovatiopr.payments.shared.domain.Money;

/** Expected business failures owned by the Ledger bounded context. */
public sealed interface LedgerError extends DomainError {

    record Unbalanced(Money totalDebits, Money totalCredits) implements LedgerError {
        @Override
        public String code() {
            return "LEDGER_UNBALANCED";
        }

        @Override
        public String message() {
            return "Ledger transaction does not balance: debits %s, credits %s".formatted(totalDebits, totalCredits);
        }

        @Override
        public ErrorType type() {
            return ErrorType.BUSINESS_RULE;
        }
    }

    record MixedCurrencies() implements LedgerError {
        @Override
        public String code() {
            return "LEDGER_MIXED_CURRENCIES";
        }

        @Override
        public String message() {
            return "All entries in a ledger transaction must share one currency";
        }

        @Override
        public ErrorType type() {
            return ErrorType.BUSINESS_RULE;
        }
    }

    record TooFewEntries(int count) implements LedgerError {
        @Override
        public String code() {
            return "LEDGER_TOO_FEW_ENTRIES";
        }

        @Override
        public String message() {
            return "A ledger transaction needs at least two entries but had " + count;
        }

        @Override
        public ErrorType type() {
            return ErrorType.BUSINESS_RULE;
        }
    }

    record NotFound(String ledgerTransactionId) implements LedgerError {
        @Override
        public String code() {
            return "LEDGER_TRANSACTION_NOT_FOUND";
        }

        @Override
        public String message() {
            return "No ledger transaction exists with id " + ledgerTransactionId;
        }

        @Override
        public ErrorType type() {
            return ErrorType.NOT_FOUND;
        }
    }
}
