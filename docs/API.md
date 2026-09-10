# API reference

Base URL `http://localhost:8080` · media type `application/hal+json` · errors
`application/problem+json` (RFC 9457).

Interactive reference: **<http://localhost:8080/docs>** (Scalar, served offline).
Raw document: `/v3/api-docs`.

---

## Conventions

### Pagination

Every collection is paginated. There is no unpaginated list endpoint.

| Parameter | Default | Notes |
|---|---|---|
| `page` | `0` | Zero-based. Negative values normalise to `0` |
| `size` | `20` | Maximum `100`. Larger values are clamped, invalid ones fall back to the default |
| `sort` | per collection | A whitelisted logical field; unknown names fall back to the default |
| `direction` | `desc` | `asc` or `desc` |

Out-of-range input is **normalised, not rejected**, and the effective values are echoed in `page`:

```json
"page": { "number": 0, "size": 20, "totalElements": 231, "totalPages": 12 }
```

Ordering always includes a unique tiebreaker, so paging enumerates a collection exactly once.
Navigation links (`self`, `first`, `prev`, `next`, `last`) appear only for pages that exist, and preserve
filters, sorting and the effective size.

### Sortable fields

| Collection | Fields | Default |
|---|---|---|
| Customers | `registeredAt`, `lastName`, `email` | `registeredAt` desc |
| Transactions | `createdAt`, `amount`, `status` | `createdAt` desc |
| Statement | `recordedAt`, `amount` | `recordedAt` desc |

### Transaction filters

`type` (`TRANSFER`\|`DEPOSIT`\|`WITHDRAWAL`) · `status` (`PENDING`\|`COMPLETED`\|`FAILED`) ·
`dateFrom`, `dateTo` (ISO-8601 instants) · `minimumAmount`, `maximumAmount`.

Filters apply to both the page and its total count, and are carried into the navigation links.

### Idempotency

`POST /api/v1/transfers` **requires** an `Idempotency-Key` header: 8–255 characters of letters, digits,
`.`, `_`, `:` or `-`. A UUID is a good choice.

| Case | Response |
|---|---|
| New key | `201 Created`, money moves |
| Same key, same body | `200 OK`, original result, `"replayed": true` |
| Same key, different body | `409 Conflict`, `TRANSFER_IDEMPOTENCY_KEY_REUSED` |

A retried HTTP request never moves money twice, including when retries arrive concurrently.

### Errors

```json
{
  "type": "https://api.payments.local/problems/account_insufficient_funds",
  "title": "Business rule violated",
  "status": 422,
  "detail": "Account 9f3a… has 120.00 USD available but 250.00 USD was requested",
  "code": "ACCOUNT_INSUFFICIENT_FUNDS",
  "errors": [{ "code": "ACCOUNT_INSUFFICIENT_FUNDS", "message": "…", "type": "BUSINESS_RULE" }],
  "correlationId": "4f2c…",
  "traceId": "1a9b…"
}
```

`code` is stable and safe to branch on. `400` malformed request · `404` not found · `409` conflict ·
`422` domain rule violated · `500` unexpected.

`X-Correlation-Id` is accepted on any request and echoed on the response.

---

## Customers

### `POST /api/v1/customers` → `201`

```json
{ "firstName": "John", "lastName": "Doe", "email": "john.doe@example.com" }
```

`400` invalid body · `409` `CUSTOMER_EMAIL_ALREADY_REGISTERED`

### `GET /api/v1/customers/{id}` → `200`

Links: `self`, `accounts`.

### `GET /api/v1/customers?page&size&sort&direction` → `200`

Each row carries `accountCount`, computed on the read side.

---

## Accounts

### `POST /api/v1/accounts` → `201`

```json
{ "customerId": "…", "currency": "USD" }
```

A new account opens **empty**. Fund it with a deposit — that is what writes the ledger postings, so every
balance is explained by the ledger. `404` if the customer does not exist.

### `GET /api/v1/accounts/{id}` → `200`

```json
{
  "id": "…", "accountNumber": "466785330778", "currency": "USD",
  "balance": 750.00, "status": "ACTIVE",
  "_links": {
    "self":         { "href": "/api/v1/accounts/…" },
    "balance":      { "href": "/api/v1/accounts/…/balance" },
    "transactions": { "href": "/api/v1/accounts/…/transactions" },
    "statement":    { "href": "/api/v1/accounts/…/statement" },
    "customer":     { "href": "/api/v1/customers/…" },
    "deposit":      { "href": "/api/v1/accounts/…/deposits" },
    "withdraw":     { "href": "/api/v1/accounts/…/withdrawals" },
    "transfer":     { "href": "/api/v1/transfers" },
    "freeze":       { "href": "/api/v1/accounts/…/freeze" }
  }
}
```

**Links follow state.** A `FROZEN` account offers no `deposit`, `withdraw` or `transfer` — only read
links and `unfreeze`. A `CLOSED` account offers read links alone. `close` appears only at a zero balance,
because the domain refuses otherwise.

### `GET /api/v1/accounts/{id}/balance` → `200`

### `POST /api/v1/accounts/{id}/freeze` · `/unfreeze` · `/close` → `200`

Return the full account representation, so the response already advertises the new state's affordances.
`close` returns `422 ACCOUNT_NOT_EMPTY` on a funded account.

---

## Money movement

### `POST /api/v1/accounts/{id}/deposits` → `201`

```json
{ "amount": 500.00, "currency": "USD", "reference": "Opening deposit" }
```

### `POST /api/v1/accounts/{id}/withdrawals` → `201`

Same body. `422 ACCOUNT_INSUFFICIENT_FUNDS` when the balance is too low.

### `POST /api/v1/transfers` → `201` (or `200` when replayed)

Header: `Idempotency-Key` (required).

```json
{
  "sourceAccountId": "…",
  "destinationAccountId": "…",
  "amount": 250.00,
  "currency": "USD",
  "reference": "Rent payment"
}
```

Response carries both resulting balances and a `Location` header.

| Status | Cause |
|---|---|
| `400` | Missing/invalid `Idempotency-Key`, or an invalid body |
| `404` | Account does not exist |
| `409` | Key reused with a different request |
| `422` | `ACCOUNT_INSUFFICIENT_FUNDS`, `ACCOUNT_FROZEN`, `ACCOUNT_CLOSED`, `TRANSFER_SAME_ACCOUNT`, `MONEY_CURRENCY_MISMATCH` |

Both accounts are locked in a deterministic order for the duration, so concurrent transfers cannot
overdraw and opposing transfers cannot deadlock.

---

## Transactions and statements

### `GET /api/v1/transactions/{id}` → `200`

### `GET /api/v1/transactions?page&size&sort&direction&type&status&dateFrom&dateTo&minimumAmount&maximumAmount` → `200`

### `GET /api/v1/accounts/{id}/transactions?…` → `200`

History for one account, matching it as either source or destination. `404` — not an empty page — when
the account does not exist.

### `GET /api/v1/accounts/{id}/statement?page&size&from&to` → `200`

The ledger view: one line per posting, each with a `runningBalance` computed by a SQL window function
over the account's whole history. The newest line's running balance always equals the account balance.

```json
{
  "_embedded": { "statementLineResourceList": [{
      "direction": "DEBIT", "amount": 250.00, "currency": "USD",
      "runningBalance": 750.00, "description": "Rent payment",
      "recordedAt": "2026-01-15T10:00:00Z",
      "_links": { "transaction": { "href": "/api/v1/transactions/…" } }
  }]},
  "page": { "number": 0, "size": 20, "totalElements": 84, "totalPages": 5 }
}
```

---

## Operations

| Endpoint | Purpose |
|---|---|
| `/docs` | Scalar API reference (offline) |
| `/v3/api-docs` | OpenAPI document |
| `/actuator/health` | Liveness and readiness probes |
| `/actuator/metrics` · `/actuator/prometheus` | Micrometer metrics |
| `/actuator/modulith` | Live module structure and dependency graph |
