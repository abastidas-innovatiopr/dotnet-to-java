package com.innovatiopr.payments.shared.domain;

import java.time.Instant;
import java.util.UUID;

/**
 * A fact that has already happened inside the domain.
 *
 * <p>Names are always past tense ({@code MoneyDebited}, {@code TransferCompleted}). {@code ProcessPayment}
 * or {@code CreateTransfer} would be commands — a request that something <em>should</em> happen — and
 * commands are not events.
 *
 * <p>Implementations are plain records with no framework annotations. Spring Modulith can publish any
 * object as an application event, so nothing here needs to know that Spring exists.
 */
public interface DomainEvent {

    /** Unique identity of this event occurrence; the idempotency key for downstream consumers. */
    UUID eventId();

    /** When the fact occurred, sourced from an injected {@link java.time.Clock} rather than {@code now()}. */
    Instant occurredAt();

    /** Identity of the aggregate that produced the event, as a string so the contract stays type-agnostic. */
    String aggregateId();

    /** Stable event name. Defaults to the simple class name, which is sufficient for in-process routing. */
    default String eventType() {
        return getClass().getSimpleName();
    }
}
