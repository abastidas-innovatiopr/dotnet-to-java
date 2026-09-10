-- ============================================================================
-- Payments API - core schema
--
-- Critical domain invariants are enforced twice: once in the domain model, where they
-- produce a meaningful DomainError, and once here, where they hold even against a
-- defective code path, a bad migration or a manual UPDATE. The domain check gives a good
-- error message; the database check gives the guarantee.
--
-- Monetary values are NUMERIC(19, 4): exact decimal arithmetic, four fractional digits so
-- that three-minor-unit currencies (BHD, KWD) fit, and fifteen integral digits of headroom.
-- No money column is ever a float or a double.
-- ============================================================================

-- ----------------------------------------------------------------------------
-- Customers
-- ----------------------------------------------------------------------------
CREATE TABLE customers
(
    id            UUID         NOT NULL,
    first_name    VARCHAR(100) NOT NULL,
    last_name     VARCHAR(100) NOT NULL,
    email         VARCHAR(254) NOT NULL,
    registered_at TIMESTAMP WITH TIME ZONE NOT NULL,
    version       BIGINT       NOT NULL DEFAULT 0,

    CONSTRAINT pk_customers PRIMARY KEY (id),
    CONSTRAINT uq_customers_email UNIQUE (email),
    CONSTRAINT ck_customers_names_present CHECK (length(trim(first_name)) > 0 AND length(trim(last_name)) > 0),
    CONSTRAINT ck_customers_email_present CHECK (length(trim(email)) > 0)
);

-- Deterministic ordering for the customer list: registered_at is not unique, so id breaks ties.
CREATE INDEX idx_customers_registered_at ON customers (registered_at DESC, id DESC);

-- ----------------------------------------------------------------------------
-- Accounts
-- ----------------------------------------------------------------------------
CREATE TABLE accounts
(
    id             UUID           NOT NULL,
    customer_id    UUID           NOT NULL,
    account_number VARCHAR(12)    NOT NULL,
    currency       VARCHAR(3)     NOT NULL,
    balance        NUMERIC(19, 4) NOT NULL,
    status         VARCHAR(16)    NOT NULL,
    opened_at      TIMESTAMP WITH TIME ZONE NOT NULL,
    version        BIGINT         NOT NULL DEFAULT 0,

    CONSTRAINT pk_accounts PRIMARY KEY (id),
    CONSTRAINT fk_accounts_customer FOREIGN KEY (customer_id) REFERENCES customers (id),
    CONSTRAINT uq_accounts_number UNIQUE (account_number),

    -- Account.debit() rejects an overdraft; this makes a negative balance unrepresentable.
    CONSTRAINT ck_accounts_balance_non_negative CHECK (balance >= 0),
    CONSTRAINT ck_accounts_status CHECK (status IN ('ACTIVE', 'FROZEN', 'CLOSED')),
    CONSTRAINT ck_accounts_currency_iso CHECK (currency ~ '^[A-Z]{3}$'),
    CONSTRAINT ck_accounts_number_digits CHECK (account_number ~ '^[0-9]{12}$')
);

CREATE INDEX idx_accounts_customer ON accounts (customer_id);

-- ----------------------------------------------------------------------------
-- Payment transactions
-- ----------------------------------------------------------------------------
CREATE TABLE payment_transactions
(
    id                     UUID           NOT NULL,
    type                   VARCHAR(16)    NOT NULL,
    source_account_id      UUID,
    destination_account_id UUID,
    amount                 NUMERIC(19, 4) NOT NULL,
    currency               VARCHAR(3)     NOT NULL,
    status                 VARCHAR(16)    NOT NULL,
    reference              VARCHAR(140)   NOT NULL DEFAULT '',
    created_at             TIMESTAMP WITH TIME ZONE NOT NULL,
    completed_at           TIMESTAMP WITH TIME ZONE,
    failure_code           VARCHAR(64),
    version                BIGINT         NOT NULL DEFAULT 0,

    CONSTRAINT pk_payment_transactions PRIMARY KEY (id),
    CONSTRAINT fk_pt_source FOREIGN KEY (source_account_id) REFERENCES accounts (id),
    CONSTRAINT fk_pt_destination FOREIGN KEY (destination_account_id) REFERENCES accounts (id),

    CONSTRAINT ck_pt_amount_positive CHECK (amount > 0),
    CONSTRAINT ck_pt_type CHECK (type IN ('TRANSFER', 'DEPOSIT', 'WITHDRAWAL')),
    CONSTRAINT ck_pt_status CHECK (status IN ('PENDING', 'COMPLETED', 'FAILED')),
    CONSTRAINT ck_pt_currency_iso CHECK (currency ~ '^[A-Z]{3}$'),

    -- PaymentTransaction.initiateTransfer() rejects a self-transfer; so does the database.
    CONSTRAINT ck_pt_accounts_differ CHECK (source_account_id IS DISTINCT FROM destination_account_id),

    -- Each transaction type has exactly the legs it should.
    CONSTRAINT ck_pt_legs_match_type CHECK (
        (type = 'TRANSFER' AND source_account_id IS NOT NULL AND destination_account_id IS NOT NULL) OR
        (type = 'DEPOSIT' AND source_account_id IS NULL AND destination_account_id IS NOT NULL) OR
        (type = 'WITHDRAWAL' AND source_account_id IS NOT NULL AND destination_account_id IS NULL)
    ),

    -- A terminal transaction has a completion timestamp; a pending one does not.
    CONSTRAINT ck_pt_completed_at_matches_status CHECK (
        (status = 'PENDING' AND completed_at IS NULL) OR
        (status IN ('COMPLETED', 'FAILED') AND completed_at IS NOT NULL)
    )
);

-- Composite indexes matching the exact ORDER BY used for pagination, tiebreaker included,
-- so deep pages stay index-only rather than degenerating into a sort of the whole table.
CREATE INDEX idx_pt_created_at ON payment_transactions (created_at DESC, id DESC);
CREATE INDEX idx_pt_source ON payment_transactions (source_account_id, created_at DESC, id DESC);
CREATE INDEX idx_pt_destination ON payment_transactions (destination_account_id, created_at DESC, id DESC);
CREATE INDEX idx_pt_status ON payment_transactions (status);

-- ----------------------------------------------------------------------------
-- Ledger
-- ----------------------------------------------------------------------------
CREATE TABLE ledger_transactions
(
    id                UUID           NOT NULL,
    posting_reference UUID           NOT NULL,
    description       VARCHAR(140)   NOT NULL DEFAULT '',
    currency          VARCHAR(3)     NOT NULL,
    total_debits      NUMERIC(19, 4) NOT NULL,
    total_credits     NUMERIC(19, 4) NOT NULL,
    recorded_at       TIMESTAMP WITH TIME ZONE NOT NULL,

    CONSTRAINT pk_ledger_transactions PRIMARY KEY (id),

    -- One ledger transaction per business operation. This unique index is a second line of
    -- defence for idempotency: even if a retry slipped past the idempotency table, it could
    -- not write a second set of postings for the same operation.
    CONSTRAINT uq_ledger_posting_reference UNIQUE (posting_reference),

    -- The defining invariant of double-entry bookkeeping, enforced by the database itself.
    CONSTRAINT ck_ledger_balanced CHECK (total_debits = total_credits),
    CONSTRAINT ck_ledger_totals_positive CHECK (total_debits > 0),
    CONSTRAINT ck_ledger_currency_iso CHECK (currency ~ '^[A-Z]{3}$')
);

CREATE INDEX idx_ledger_recorded_at ON ledger_transactions (recorded_at DESC, id DESC);

CREATE TABLE ledger_entries
(
    id                    UUID           NOT NULL,
    ledger_transaction_id UUID           NOT NULL,
    account_id            UUID,
    external_account      VARCHAR(64),
    direction             VARCHAR(6)     NOT NULL,
    amount                NUMERIC(19, 4) NOT NULL,
    currency              VARCHAR(3)     NOT NULL,
    recorded_at           TIMESTAMP WITH TIME ZONE NOT NULL,

    CONSTRAINT pk_ledger_entries PRIMARY KEY (id),
    CONSTRAINT fk_le_transaction FOREIGN KEY (ledger_transaction_id) REFERENCES ledger_transactions (id),
    CONSTRAINT fk_le_account FOREIGN KEY (account_id) REFERENCES accounts (id),

    CONSTRAINT ck_le_direction CHECK (direction IN ('DEBIT', 'CREDIT')),
    CONSTRAINT ck_le_amount_positive CHECK (amount > 0),
    CONSTRAINT ck_le_currency_iso CHECK (currency ~ '^[A-Z]{3}$'),

    -- LedgerAccountRef is a sealed interface with exactly two cases; this is that type in SQL.
    CONSTRAINT ck_le_exactly_one_counterparty CHECK (num_nonnulls(account_id, external_account) = 1)
);

-- Drives the account statement query, including its running-balance window function.
CREATE INDEX idx_le_account_recorded ON ledger_entries (account_id, recorded_at DESC, id DESC);
CREATE INDEX idx_le_transaction ON ledger_entries (ledger_transaction_id);

-- ----------------------------------------------------------------------------
-- Idempotency
-- ----------------------------------------------------------------------------
CREATE TABLE idempotency_records
(
    idempotency_key VARCHAR(255) NOT NULL,
    request_hash    VARCHAR(64)  NOT NULL,
    transaction_id  UUID         NOT NULL,
    response_status INTEGER      NOT NULL,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL,

    -- The primary key IS the concurrency control. Two requests carrying the same key race to
    -- insert this row; PostgreSQL makes the second one wait for the first to commit and then
    -- rejects it, which rolls its whole transaction back. That is what makes a retried
    -- HTTP request unable to move money twice.
    CONSTRAINT pk_idempotency_records PRIMARY KEY (idempotency_key),
    CONSTRAINT fk_idempotency_transaction FOREIGN KEY (transaction_id) REFERENCES payment_transactions (id),
    CONSTRAINT ck_idempotency_hash_length CHECK (length(request_hash) = 64)
);

CREATE INDEX idx_idempotency_created_at ON idempotency_records (created_at);
