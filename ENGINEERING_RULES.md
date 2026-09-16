# Payments API — Engineering Rules

Normative rules for this repository. `docs/ARCHITECTURE.md` describes how the system *is* built and
`README.md` explains the Java and Spring concepts behind it; this file states what a change **must** do to
be accepted, and is the checklist a reviewer is entitled to reject against.

Each rule carries the same four parts:

- **Rule** — the obligation, stated so it can be answered yes/no.
- **Why** — the failure it exists to prevent, with the concrete case that produced it.
- **How** — what conformance looks like in this codebase.
- **Review check** — what a reviewer looks for on a PR.

Rules are numbered and append-only: a rule that stops applying is struck through with the reason, never
silently deleted, so a PR comment citing "R2" still resolves years later.

This set is ported from `ddec-platform-api`, the .NET service this application mirrors, so a reviewer who
knows one repository can review the other. R1–R8 keep their meaning and their numbers. R9 and R10 are
Java-specific and were both learned the hard way here.

## Index

| ID | Rule | Enforced by |
|----|------|-------------|
| [R1](#r1--evaluate-a-central-home-before-writing-a-helper-locally) | Evaluate a central home before writing a helper locally | Review |
| [R2](#r2--filter-in-the-database-not-in-memory) | Filter in the database, not in memory | Review |
| [R3](#r3--single-source-of-truth-constants) | Single-source-of-truth constants | Review |
| [R4](#r4--mutations-persist-through-one-transaction) | Mutations persist through one transaction | Review |
| [R5](#r5--caller-identity-comes-from-the-token-never-from-the-request) | Caller identity comes from the token, never from the request | Review *(forward-looking)* |
| [R6](#r6--dependency-direction-is-inward) | Dependency direction is inward | `ArchitectureTest`, `ModularityTest`, the POMs |
| [R7](#r7--format-tall-not-wide) | Format tall, not wide | Review |
| [R8](#r8--start-with-tdd) | Start with TDD — write the failing test first | Review |
| [R9](#r9--expected-failures-are-thrown-never-returned) | Expected failures are thrown, never returned | `ArchitectureTest` |
| [R10](#r10--every-operation-declares-its-operationid) | Every operation declares its `operationId` | `OpenApiDocumentIT` |

---

## R1 — Evaluate a central home before writing a helper locally

**Rule.** Before adding a helper next to its first caller, check whether it belongs in a shared package.

**Why.** A helper copied into a second slice is a helper that will disagree with itself. Lenient query
parsing is the example: it existed once per endpoint, each with slightly different blank handling, until
it became `QueryValues`.

**How.** `shared/api/` for HTTP concerns (`ApiPaths`, `ApiResponses`, `ProblemDetails`, `PageQuery`,
`QueryValues`, `PagedResources`), `shared/application/` for cross-feature use-case types (`PageRequest`,
`PageResult`, `SortSpec`, `RequestHasher`), `shared/domain/` for the shared kernel. The shared kernel is a
liability as well as a convenience — everything in it couples to everything using it — so it stays small
and never holds a business rule.

**Review check.** A new private static method that is a near-duplicate of one in another slice.

---

## R2 — Filter in the database, not in memory

**Rule.** Narrow a result set in SQL. Never read a large page and filter, sort or count it in Java.

**Why.** It is correct on a developer's dataset and wrong in production, in two ways at once: the memory
grows with the table rather than the page, and the reported `totalPages` describes the unfiltered set, so
a client paging through a filtered collection is told the wrong size.

**How.** The read models (`JdbcTransactionReadModel`, `JdbcStatementReadModel`, `JdbcCustomerReadModel`)
apply every filter to both the page query and its count query. Sort fields go through `SortColumns`, an
allow-list — a user-supplied sort field is a SQL injection vector, and rejecting unknown values by falling
back to the default is what makes lenient parsing safe. The statement's `runningBalance` is a window
function precisely because it cannot be computed from one page.

**Review check.** `.stream().filter(...)` over something that came from a repository. A `PageRequest` with
a large `size` followed by narrowing.

---

## R3 — Single-source-of-truth constants

**Rule.** A path, a code, a version or a column name exists in exactly one place.

**Why.** Two copies drift, and the copy that drifts is usually the one nobody is looking at — a link
pointing at a renamed route, an error code the client branches on that no longer matches what the server
sends.

**How.** URI templates live in `ApiPaths` and are used both by `@RequestMapping` and by the assemblers.
Error codes are paired with their messages in the `*Errors` factories and nowhere else. Dependency
versions live in the parent POM's `dependencyManagement` and `<properties>` — the Java equivalent of
`Directory.Packages.props`; a module POM never carries a version. OpenAPI tag names live in `OpenApiDocs`.

**Review check.** A literal path string in a controller or assembler. A version in a module POM. An error
code spelled out anywhere but its factory.

---

## R4 — Mutations persist through one transaction

**Rule.** Everything a use case changes commits together or not at all. The transaction boundary is the
application handler, never a controller and never a domain object.

**Why.** A transfer that moved balances but failed to write its ledger postings leaves the books wrong,
and no retry fixes an inconsistency a client has already been told succeeded. This is not hypothetical
here: see R9 for the exact bug.

**How.** `@Transactional` on the handler. Cross-module posting APIs (`AccountsApi`, `LedgerApi`) are
`@Transactional(propagation = MANDATORY)`, so calling one outside a transaction fails immediately instead
of silently committing one leg on its own. Hibernate dirty-checks managed aggregates, so a mutated
`Account` needs no explicit save call — which is exactly why a failure *after* the mutation must throw.

Mind the proxy: `@Transactional` is applied by a proxy, so a call from one method of a bean to another
method of the **same** bean bypasses it entirely and the annotation silently does nothing. That is why
`TransferMoneyHandler`, `TransferMoneyOperation` and `TransferReplayReader` are three beans.

**Review check.** A second `save` call that could have been one transaction. `@Transactional` on a
controller (ArchUnit already fails this). A private method annotated `@Transactional` — it does nothing.

---

## R5 — Caller identity comes from the token, never from the request

**Rule.** When authentication exists, who the caller is comes from the verified credential. A field in the
request body or a query parameter is a claim, not an identity.

**Why.** An endpoint that trusts a `customerId` in the body lets any caller act as any customer. It is the
single most common authorization defect, and it reads as perfectly ordinary code.

**How.** *Forward-looking: this application has no authentication yet.* When it arrives, identity belongs
behind a port in `shared/application/` implemented in the API layer, and resource authorization belongs in
the application layer — not in a controller, which cannot be reused by a message consumer or a scheduled
job. `ForbiddenException` already exists and already maps to 403.

**Review check.** A handler that reads an owner id from a command instead of from the caller.

---

## R6 — Dependency direction is inward

**Rule.** `api → application → domain` and `infrastructure → application → domain`. The domain references
nothing. No transport or persistence type crosses inward.

**Why.** It is the property that keeps the domain and application layers testable without a web server or
a database — the domain suite runs in milliseconds because there is no framework on its classpath — and it
degrades one import at a time.

**How.** The layers are Maven modules, so the dependency rule is enforced by the compiler before any test
runs: the domain module cannot import Spring because Spring is not on its classpath. `payments-api` and
`payments-infrastructure` cannot see each other at all, which is stricter than the .NET reference, where
`API → Infrastructure`. `ArchitectureTest` covers what a POM cannot express, and `ModularityTest` covers
the bounded-context boundaries within a layer.

**Review check.** Enforced automatically — a violation fails the build, so this rule needs no manual
check. Adding a module dependency that trips it is a design question, not a test to relax.

---

## R7 — Format tall, not wide

**Rule.** When a construct does not fit comfortably on one line, break it downward — one item per line —
rather than letting the line run long.

**Why.** A tall layout diffs cleanly: adding or changing one argument touches one line, so review sees the
actual change instead of a re-wrapped paragraph. It also survives side-by-side diffs without horizontal
scrolling, and keeps blame attributable per item.

**How.**

```java
// Prefer
public static PaymentTransaction initiateTransfer(
        TransactionId id,
        AccountId source,
        AccountId destination,
        Money amount,
        TransactionReference reference,
        Instant now)

// Avoid
public static PaymentTransaction initiateTransfer(TransactionId id, AccountId source, AccountId destination, Money amount, TransactionReference reference, Instant now)
```

Once a list is broken across lines, break *every* item onto its own line rather than packing two or three
per line — a partial break gives up the per-item diff without shortening anything. A short call that
already fits stays on one line; this rule is about what to do when it does not.

**Review check.** A line that wraps in a side-by-side diff, or a multi-line argument list with more than
one argument sharing a line.

---

## R8 — Start with TDD

**Rule.** Every behavioural change **starts** with a failing test. Write the test, watch it fail for the
right reason, then write the minimum code that makes it pass, then refactor with the test green. This is
mandatory, not a preference, and it applies to **every contributor — human or AI agent**.

**Order, explicitly:**

1. **Red** — add a test that fails because the behaviour does not exist yet. Run it and confirm the
   failure message is the one you expect. A test that passes on first run proves nothing.
2. **Green** — write the least code that turns it green. Do not add behaviour no test asked for.
3. **Refactor** — improve names, structure and duplication while the suite stays green.

**Why.** A test written after the code is shaped by the implementation it was written against, so it
documents what the code *does* rather than what it *should do*, and it cannot fail for the reason that
matters. `TransferRollbackIT` is the case in point: the bug it guards was invisible to every existing
transfer test, because they all failed *before* any balance was staged. Only writing down "a failure after
the money moved must unwind it" as a test made the defect visible.

**How.** Match the test to the layer: domain behaviour → `payments-domain`; use cases → 
`payments-application`; persistence and read models → `payments-infrastructure`; endpoints, wiring,
layering and the whole stack → `payments-bootstrap`. `./mvnw verify` must be green before a PR is opened;
the integration tests need Docker running for Testcontainers, and a connection failure there means the
harness is not up, not that the change is fine.

Legitimate exceptions are narrow — pure renames, comment and documentation edits, formatting and
dependency bumps. "It is only a small change" is not one of them, and neither is "the behaviour is hard to
test": if it is hard to test, that is a design finding, so raise it rather than skipping the test.

**Review check.** A PR that changes behaviour with no accompanying test, or whose tests could not have
failed before the change.

---

## R9 — Expected failures are thrown, never returned

**Rule.** A business failure leaves a handler as an exception — `ValidationException`,
`NotFoundException`, `ConflictException` or `DomainException`. It is never returned as a value a caller
can ignore, and code inside a transaction never catches one and carries on.

**Why.** This is the rule that cost real money in a test. An earlier design returned a `Result<T>`, and
**a `@Transactional` method that returns a failure commits.** `TransferMoneyOperation` stages both balance
changes in `postTransfer` and only then writes the ledger, so a ledger failure *returned* rather than
thrown persisted the moved money with no ledger rows and no transaction to explain it — the balance and
the ledger disagreed permanently, in an application whose entire purpose is an auditable ledger.

Throwing makes the rollback Spring's job. The second half of the rule follows from the first: once a
`MANDATORY` adapter has thrown, the shared transaction is marked rollback-only, so catching the failure
and proceeding produces `UnexpectedRollbackException` at commit — a confusing error far from its cause.

**How.** Aggregates and handlers throw. Each context owns a `*Errors` final class pairing a code with its
message; the exception *type* carries the HTTP meaning, so there is no second classification to disagree
with it. `ApiExceptionHandler` is the one place the mapping to a status lives.

`TransferMoneyHandler` is the single place that catches a failure, and it deliberately carries no
`@Transactional` so the catch runs after the operation's transaction has unwound. Its `catch` is narrowly
typed to `DuplicateIdempotencyKeyException`: widening it would read a genuine insufficient-funds failure
as a lost key race.

**Review check.** A method returning a failure-shaped value. A `catch` of a domain exception anywhere
inside a transaction. `TransferRollbackIT` and four ArchUnit rules fail the build on the structural half.

---

## R10 — Every operation declares its `operationId`

**Rule.** Every controller method carries `@Operation(operationId = "…")` with the published id.

**Why.** springdoc derives an id from the Java method name when one is not given, so two methods named
`getById` become `getById` and `getById_1`. Renaming a Java method then silently renames the operation and
breaks every generated client, while every "is it documented?" assertion stays green. It is a contract
change disguised as a refactor.

**How.** The 16 published ids are listed in `OpenApiDocumentIT.EXPECTED_OPERATION_IDS` and asserted
exactly, not merely for being non-null. Response media types are *not* declared per operation — 
`PaymentsOperationCustomizer` applies `application/problem+json` to every 4xx and 5xx and
`application/hal+json` to successes, so a forgotten `@Content` cannot produce a document that contradicts
what the server sends.

**Review check.** A new endpoint without an explicit `operationId`, or one added without a matching entry
in `EXPECTED_OPERATION_IDS`.
