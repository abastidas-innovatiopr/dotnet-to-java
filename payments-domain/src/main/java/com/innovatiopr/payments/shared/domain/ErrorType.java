package com.innovatiopr.payments.shared.domain;

/**
 * Domain-level classification of a {@link DomainError}.
 *
 * <p>This is deliberately <em>not</em> an HTTP status code. It is the same idea as {@code ErrorOr}'s
 * {@code ErrorType} in .NET: the domain states <em>what kind</em> of failure occurred, and an outer
 * layer decides how to represent it in a given transport. The mapping to HTTP lives in
 * {@code shared.api.ProblemDetailFactory} — never here.
 */
public enum ErrorType {

    /** Input could not be accepted at all (malformed, out of range, structurally invalid). */
    VALIDATION,

    /** A referenced aggregate or entity does not exist. */
    NOT_FOUND,

    /** The request conflicts with existing state (duplicate key, concurrent modification). */
    CONFLICT,

    /** Input was well formed but an invariant of the domain forbids the operation. */
    BUSINESS_RULE
}
