package com.innovatiopr.payments.ledger.infrastructure;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Persistence representation of a single ledger posting.
 *
 * <p>{@code accountId} and {@code externalAccount} are the two arms of the {@code LedgerAccountRef} sealed
 * interface, flattened into nullable columns with a {@code CHECK} that exactly one is set.
 */
@Entity
@Table(name = "ledger_entries")
class LedgerEntryJpaEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "account_id", updatable = false)
    private UUID accountId;

    @Column(name = "external_account", updatable = false, length = 64)
    private String externalAccount;

    @Column(name = "direction", nullable = false, updatable = false, length = 6)
    private String direction;

    @Column(name = "amount", nullable = false, updatable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    @Column(name = "currency", nullable = false, updatable = false, length = 3)
    private String currency;

    @Column(name = "recorded_at", nullable = false, updatable = false)
    private Instant recordedAt;

    protected LedgerEntryJpaEntity() {
        // required by Hibernate
    }

    LedgerEntryJpaEntity(UUID id, UUID accountId, String externalAccount, String direction, BigDecimal amount,
                         String currency, Instant recordedAt) {
        this.id = id;
        this.accountId = accountId;
        this.externalAccount = externalAccount;
        this.direction = direction;
        this.amount = amount;
        this.currency = currency;
        this.recordedAt = recordedAt;
    }

    UUID getId() {
        return id;
    }

    UUID getAccountId() {
        return accountId;
    }

    String getExternalAccount() {
        return externalAccount;
    }

    String getDirection() {
        return direction;
    }

    BigDecimal getAmount() {
        return amount;
    }

    String getCurrency() {
        return currency;
    }

    Instant getRecordedAt() {
        return recordedAt;
    }
}
