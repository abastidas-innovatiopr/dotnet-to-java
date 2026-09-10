package com.innovatiopr.payments.shared.application;

import com.innovatiopr.payments.shared.domain.AggregateRoot;
import com.innovatiopr.payments.shared.domain.DomainEvent;

import java.util.Collection;

/**
 * Outbound port for publishing recorded domain events.
 *
 * <p>Declared in the application layer so that no aggregate and no use case ever imports Spring. The
 * adapter ({@code shared.infrastructure.SpringDomainEventPublisher}) delegates to Spring's
 * {@code ApplicationEventPublisher}, which Spring Modulith augments with a persistent publication log.
 */
public interface DomainEventPublisher {

    void publish(Collection<? extends DomainEvent> events);

    /**
     * Drains each aggregate's recorded events and publishes them. Draining means an aggregate cannot emit
     * the same event twice if a handler is invoked again in the same transaction.
     */
    void publishFrom(AggregateRoot<?>... aggregates);
}
