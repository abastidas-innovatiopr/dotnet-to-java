package com.innovatiopr.payments.shared.infrastructure;

import com.innovatiopr.payments.shared.application.DomainEventPublisher;
import com.innovatiopr.payments.shared.domain.AggregateRoot;
import com.innovatiopr.payments.shared.domain.DomainEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import java.util.Collection;

/**
 * Adapter from the {@link DomainEventPublisher} port to Spring's event infrastructure.
 *
 * <h2>Where events go, and when</h2>
 * {@code publishEvent} does <em>not</em> deliver anything yet. Listeners in this application are annotated
 * {@code @ApplicationModuleListener} (Spring Modulith), which expands to:
 * <ul>
 *   <li>{@code @TransactionalEventListener(phase = AFTER_COMMIT)} — the listener runs only once the
 *       publishing transaction has committed. A listener can therefore never see state that later rolls
 *       back, and can never roll the publisher back either.</li>
 *   <li>{@code @Async} — it runs on a different thread, so a slow subscriber cannot lengthen the HTTP
 *       request that produced the event.</li>
 *   <li>{@code @Transactional(propagation = REQUIRES_NEW)} — the listener gets its own transaction, since
 *       the original one is finished by the time it runs.</li>
 * </ul>
 *
 * <h2>Why after commit, and what that costs</h2>
 * Anything that must be atomic with the state change is a direct synchronous call, not an event: the
 * ledger postings for a transfer are written inside the transfer's transaction, through
 * {@code LedgerApi}. Events are for consequences that may lag — notifications, audit projections,
 * analytics.
 *
 * <p>The cost is a gap between commit and delivery in which the process could die. Spring Modulith closes
 * it with the {@code event_publication} table: each listener's pending invocation is written inside the
 * publishing transaction and marked complete after it succeeds. That is the transactional-outbox pattern,
 * and it turns delivery into at-least-once. Handlers therefore have to be idempotent — an incomplete
 * publication that is resubmitted after a restart will be delivered a second time.
 */
@Component
class SpringDomainEventPublisher implements DomainEventPublisher {

    private final ApplicationEventPublisher publisher;

    SpringDomainEventPublisher(ApplicationEventPublisher publisher) {
        this.publisher = publisher;
    }

    @Override
    public void publish(Collection<? extends DomainEvent> events) {
        events.forEach(publisher::publishEvent);
    }

    @Override
    public void publishFrom(AggregateRoot<?>... aggregates) {
        for (AggregateRoot<?> aggregate : aggregates) {
            // Draining rather than reading means an aggregate saved twice in one use case cannot
            // republish the same fact.
            publish(aggregate.drainDomainEvents());
        }
    }
}
