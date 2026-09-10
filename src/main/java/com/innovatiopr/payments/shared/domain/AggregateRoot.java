package com.innovatiopr.payments.shared.domain;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Base class for aggregate roots: the single entry point through which an aggregate's invariants are
 * enforced, and the only object in the aggregate that outside code may hold a reference to.
 *
 * <p>Aggregates <em>record</em> events; they never publish them. Publication requires knowing about
 * transactions and an event bus, and neither belongs in the domain. The application layer drains
 * {@link #domainEvents()} after the aggregate has been persisted and hands them to a publisher port. This
 * is why no aggregate ever touches {@code ApplicationEventPublisher} directly.
 *
 * @param <ID> the aggregate's identity type — always a value object, never a bare {@code UUID}
 */
public abstract class AggregateRoot<ID> {

    private final List<DomainEvent> domainEvents = new ArrayList<>();

    /** Identity of this aggregate. */
    public abstract ID id();

    /** Records a fact produced by a state transition. Visible only to subclasses. */
    protected final void raise(DomainEvent event) {
        domainEvents.add(Objects.requireNonNull(event, "event"));
    }

    /** Defensive copy of the events recorded so far. */
    public final List<DomainEvent> domainEvents() {
        return List.copyOf(domainEvents);
    }

    public final void clearDomainEvents() {
        domainEvents.clear();
    }

    /**
     * Returns the recorded events and clears them in one step, so an aggregate cannot be published twice.
     */
    public final List<DomainEvent> drainDomainEvents() {
        List<DomainEvent> drained = List.copyOf(domainEvents);
        domainEvents.clear();
        return drained;
    }
}
