package com.innovatiopr.payments.ledger.infrastructure;

import com.innovatiopr.payments.accounts.AccountId;
import com.innovatiopr.payments.ledger.LedgerTransactionId;
import com.innovatiopr.payments.ledger.PostingReference;
import com.innovatiopr.payments.ledger.domain.EntryDirection;
import com.innovatiopr.payments.ledger.domain.LedgerAccountRef;
import com.innovatiopr.payments.ledger.domain.LedgerEntry;
import com.innovatiopr.payments.ledger.domain.LedgerEntryId;
import com.innovatiopr.payments.ledger.domain.LedgerTransaction;
import com.innovatiopr.payments.shared.domain.Money;

import java.util.Currency;
import java.util.List;

final class LedgerPersistenceMapper {

    private LedgerPersistenceMapper() {
    }

    static LedgerTransaction toDomain(LedgerTransactionJpaEntity entity) {
        Currency currency = Currency.getInstance(entity.getCurrency());
        List<LedgerEntry> entries = entity.getEntries().stream()
                .map(row -> toDomain(row, currency))
                .toList();
        return LedgerTransaction.reconstitute(
                LedgerTransactionId.of(entity.getId()),
                PostingReference.of(entity.getPostingReference()),
                entries,
                entity.getDescription(),
                entity.getRecordedAt());
    }

    private static LedgerEntry toDomain(LedgerEntryJpaEntity row, Currency currency) {
        LedgerAccountRef account = row.getAccountId() != null
                ? LedgerAccountRef.internal(AccountId.of(row.getAccountId()))
                : new LedgerAccountRef.External(row.getExternalAccount());
        return new LedgerEntry(
                LedgerEntryId.of(row.getId()),
                account,
                EntryDirection.valueOf(row.getDirection()),
                Money.of(row.getAmount(), currency));
    }

    static LedgerTransactionJpaEntity toNewEntity(LedgerTransaction transaction) {
        List<LedgerEntryJpaEntity> entries = transaction.entries().stream()
                .map(entry -> toNewEntity(entry, transaction.recordedAt()))
                .toList();
        return new LedgerTransactionJpaEntity(
                transaction.id().value(),
                transaction.reference().value(),
                transaction.description(),
                transaction.currency().getCurrencyCode(),
                transaction.totalDebits().amount(),
                transaction.totalCredits().amount(),
                transaction.recordedAt(),
                entries);
    }

    private static LedgerEntryJpaEntity toNewEntity(LedgerEntry entry, java.time.Instant recordedAt) {
        return new LedgerEntryJpaEntity(
                entry.id().value(),
                entry.account().internalAccountId().map(AccountId::value).orElse(null),
                entry.account().externalName().orElse(null),
                entry.direction().name(),
                entry.amount().amount(),
                entry.amount().currency().getCurrencyCode(),
                recordedAt);
    }
}
