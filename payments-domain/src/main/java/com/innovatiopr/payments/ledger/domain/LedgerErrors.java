package com.innovatiopr.payments.ledger.domain;

import com.innovatiopr.payments.shared.domain.DomainException;
import com.innovatiopr.payments.shared.domain.Money;
import com.innovatiopr.payments.shared.domain.NotFoundException;

/** Expected business failures owned by the Ledger bounded context. */
public final class LedgerErrors {

    private LedgerErrors() {
    }

    public static NotFoundException notFound(String ledgerTransactionId) {
        return new NotFoundException(
                "LEDGER_TRANSACTION_NOT_FOUND",
                "No ledger transaction exists with id " + ledgerTransactionId);
    }

    public static DomainException unbalanced(Money totalDebits, Money totalCredits) {
        return new DomainException(
                "LEDGER_UNBALANCED",
                "Ledger transaction does not balance: debits %s, credits %s".formatted(totalDebits, totalCredits));
    }

    public static DomainException mixedCurrencies() {
        return new DomainException(
                "LEDGER_MIXED_CURRENCIES",
                "All entries in a ledger transaction must share one currency");
    }

    public static DomainException tooFewEntries(int count) {
        return new DomainException(
                "LEDGER_TOO_FEW_ENTRIES",
                "A ledger transaction needs at least two entries but had " + count);
    }
}
