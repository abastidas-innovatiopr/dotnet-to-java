package com.innovatiopr.payments.shared.application;

import com.innovatiopr.payments.shared.domain.Result;

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
 * @param <C> the command type
 * @param <R> the payload produced on success
 */
public interface CommandHandler<C extends Command, R> {

    Result<R> handle(C command);
}
