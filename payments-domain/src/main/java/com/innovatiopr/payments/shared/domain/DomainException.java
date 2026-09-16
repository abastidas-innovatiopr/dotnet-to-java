package com.innovatiopr.payments.shared.domain;

import java.util.Objects;

/**
 * An invariant of the domain refused the operation.
 *
 * <p>This is the root of the application's <em>expected</em> failure model: a business rule said no.
 * It is not a programming bug and not an infrastructure fault, and it carries no stack-trace value —
 * the interesting information is the {@link #code()}, which is a published contract clients may
 * branch on.
 *
 * <h2>Why an exception and not a {@code Result<T>}</h2>
 * An earlier design returned {@code Result<T>} (a transplant of .NET's {@code ErrorOr<T>}). That forced
 * every handler into {@code if (x.isFailure()) return x.propagate();} plumbing, and — more seriously —
 * returning a failure from inside a {@code @Transactional} method <em>commits</em> the transaction,
 * so a half-applied write could be persisted. Throwing gives Spring the rollback for free and reads
 * the way Java reads.
 *
 * <p>Extends {@link IllegalStateException} for the same reason .NET's {@code DomainException} extends
 * {@code InvalidOperationException}: the object was asked to do something its current state forbids.
 *
 * <h2>Where these are constructed</h2>
 * Never inline. Each bounded context owns a {@code *Errors} factory class — {@code AccountErrors},
 * {@code CustomerErrors}, {@code LedgerErrors}, {@code TransactionErrors}, {@code TransferErrors},
 * {@code MoneyErrors} — which is the single place a code and its message are paired.
 */
public class DomainException extends IllegalStateException {

    private final String code;

    public DomainException(String code, String message) {
        super(Objects.requireNonNull(message, "message"));
        this.code = Objects.requireNonNull(code, "code");
    }

    /**
     * Stable, machine-readable identifier such as {@code ACCOUNT_INSUFFICIENT_FUNDS}.
     * Clients may branch on this value, so it is part of the public contract and must not change.
     */
    public String code() {
        return code;
    }
}
