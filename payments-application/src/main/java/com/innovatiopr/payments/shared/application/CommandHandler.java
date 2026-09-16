package com.innovatiopr.payments.shared.application;

/**
 * Handles exactly one {@link Command}.
 *
 * <p>The .NET equivalent is a Wolverine or MediatR handler. Deliberately there is <em>no</em> mediator or
 * dispatcher: endpoints inject the concrete handler type they need. That keeps the call graph statically
 * navigable ("go to implementation" works), avoids reflection at startup, and removes a layer of
 * indirection that buys nothing in a monolith.
 *
 * <p>Implementations are annotated {@code @Transactional} at the class level — see the transaction notes
 * in {@code README.md}.
 *
 * <h2>Failures are thrown, not returned</h2>
 * A business failure leaves this method as an exception ({@code DomainException}, {@code NotFoundException},
 * {@code ConflictException}, {@code ValidationException}), which the API layer renders as an RFC 9457
 * problem document. That is not only a style choice: a {@code @Transactional} method that <em>returns</em>
 * a failure commits, so a handler that had already staged a write would persist half an operation.
 * Throwing hands Spring the rollback.
 *
 * <p>The corollary is a rule for callers: code running inside a transaction must not catch one of these
 * and continue. The transaction is already marked rollback-only, so the commit would fail with
 * {@code UnexpectedRollbackException}.
 *
 * @param <C> the command type
 * @param <R> the payload produced on success
 */
public interface CommandHandler<C extends Command, R> {

    R handle(C command);
}
