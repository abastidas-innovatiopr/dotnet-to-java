package com.innovatiopr.payments.payments.transfer.application;

import com.innovatiopr.payments.payments.domain.PaymentEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

/**
 * Reacts to a completed transfer — the worked example of asynchronous, after-commit event handling.
 *
 * <h2>What {@code @ApplicationModuleListener} actually does</h2>
 * It is a composed annotation equivalent to:
 * <pre>{@code
 * @Async
 * @Transactional(propagation = REQUIRES_NEW)
 * @TransactionalEventListener(phase = AFTER_COMMIT)
 * }</pre>
 * so this method runs on another thread, in its own transaction, only once the transfer has committed.
 * A slow or failing notification cannot delay the HTTP response and cannot roll the transfer back.
 *
 * <h2>Why the ledger is not written here</h2>
 * It would be tempting to move ledger postings into a listener like this one and call it decoupling. It
 * would be wrong: after-commit means the transfer is already durable and the client has already been told
 * it succeeded, so a listener failure would leave money moved with no matching postings. Anything that
 * must be atomic with the state change is a synchronous call inside the transaction; events are for
 * consequences that are allowed to lag.
 *
 * <h2>At-least-once, so handlers must be idempotent</h2>
 * Spring Modulith records this invocation in {@code event_publication} inside the publishing transaction
 * and marks it complete when the method returns normally. If the process dies in between, the incomplete
 * row is resubmitted on restart — which means this method can run twice for one event. Sending a customer
 * two notifications is tolerable; a handler that moved money would not be.
 */
@Component
class TransferNotificationListener {

    private static final Logger log = LoggerFactory.getLogger(TransferNotificationListener.class);

    @ApplicationModuleListener
    void onTransferCompleted(PaymentEvents.TransferCompleted event) {
        // A real system would enqueue an email or a push notification here. Deliberately no external
        // service: this application must run entirely offline.
        log.info("Notification: transfer {} of {} {} completed from {} to {}",
                event.aggregateId(), event.amount(), event.currency(),
                event.sourceAccountId(), event.destinationAccountId());
    }
}
