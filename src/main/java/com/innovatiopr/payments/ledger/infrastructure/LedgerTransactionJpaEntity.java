package com.innovatiopr.payments.ledger.infrastructure;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Persistence representation of a ledger transaction and its entries.
 *
 * <p>The one place in this application where a JPA association is mapped, and it is justified: entries
 * are part of the {@code LedgerTransaction} aggregate, are always written with it, and are never loaded
 * or modified on their own. That is the criterion for mapping an association at all — never
 * "because a foreign key exists".
 *
 * <p>Cross-aggregate references stay as plain ids: {@code account_id} is a {@code UUID} column, not a
 * {@code @ManyToOne AccountJpaEntity}. Mapping it would let a ledger query drag account rows into memory,
 * blur the aggregate boundary and create exactly the object graph that makes lazy-loading surprises and
 * N+1 queries possible.
 *
 * <p>{@code total_debits} and {@code total_credits} are stored rather than derived so the balance
 * invariant can be a {@code CHECK} constraint. Derived-only, the database could not enforce it.
 */
@Entity
@Table(name = "ledger_transactions")
class LedgerTransactionJpaEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "posting_reference", nullable = false, updatable = false)
    private UUID postingReference;

    @Column(name = "description", nullable = false, length = 140)
    private String description;

    @Column(name = "currency", nullable = false, updatable = false, length = 3)
    private String currency;

    @Column(name = "total_debits", nullable = false, updatable = false, precision = 19, scale = 4)
    private BigDecimal totalDebits;

    @Column(name = "total_credits", nullable = false, updatable = false, precision = 19, scale = 4)
    private BigDecimal totalCredits;

    @Column(name = "recorded_at", nullable = false, updatable = false)
    private Instant recordedAt;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @JoinColumn(name = "ledger_transaction_id", nullable = false)
    private List<LedgerEntryJpaEntity> entries = new ArrayList<>();

    protected LedgerTransactionJpaEntity() {
        // required by Hibernate
    }

    LedgerTransactionJpaEntity(UUID id, UUID postingReference, String description, String currency,
                               BigDecimal totalDebits, BigDecimal totalCredits, Instant recordedAt,
                               List<LedgerEntryJpaEntity> entries) {
        this.id = id;
        this.postingReference = postingReference;
        this.description = description;
        this.currency = currency;
        this.totalDebits = totalDebits;
        this.totalCredits = totalCredits;
        this.recordedAt = recordedAt;
        this.entries = new ArrayList<>(entries);
    }

    UUID getId() {
        return id;
    }

    UUID getPostingReference() {
        return postingReference;
    }

    String getDescription() {
        return description;
    }

    String getCurrency() {
        return currency;
    }

    BigDecimal getTotalDebits() {
        return totalDebits;
    }

    BigDecimal getTotalCredits() {
        return totalCredits;
    }

    Instant getRecordedAt() {
        return recordedAt;
    }

    List<LedgerEntryJpaEntity> getEntries() {
        return entries;
    }
}
