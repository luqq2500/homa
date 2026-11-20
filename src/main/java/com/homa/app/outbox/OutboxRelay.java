package com.homa.app.outbox;

import com.homa.ie.port.IntegrationEvent;
import com.homa.ie.port.RelayedIE;
import java.util.concurrent.atomic.AtomicReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.PayloadApplicationEvent;
import org.springframework.core.ResolvableType;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Delivers outbox rows to the consuming contexts: at least once, in order per aggregate, and safe to run in any
 * number of instances at once.
 *
 * <p>Each row is handled in its own relay transaction, which claims it with {@code FOR UPDATE SKIP LOCKED}, publishes
 * it synchronously as a {@code RelayedIE<TheEventIE>} to the listeners in each consumer's {@code infra/event}, and
 * marks it published. The mark commits only if every listener returned, so a row is published only after delivery
 * succeeded. A listener that throws rolls the relay transaction back; the row keeps its place, its aggregate's later
 * rows wait behind it, and it is retried after a backoff. A listener runs its handler in the handler's own
 * transaction, and the handler's ProcessedMessageStore makes the retry a no-op for the handlers that had already
 * succeeded.
 */
@Component
public class OutboxRelay {

    private static final Logger LOG = LoggerFactory.getLogger(OutboxRelay.class);

    private final OutboxStore store;
    private final IntegrationEventJson json;
    private final ApplicationEventPublisher listeners;
    private final TransactionTemplate transaction;

    public OutboxRelay(OutboxStore store, IntegrationEventJson json, ApplicationEventPublisher listeners,
                       PlatformTransactionManager transactions) {
        this.store = store;
        this.json = json;
        this.listeners = listeners;
        this.transaction = new TransactionTemplate(transactions);
    }

    /**
     * Relays due rows one at a time until none is due or {@code limit} rows were handled.
     *
     * @return the number of rows handled, delivered or failed
     */
    public int relayDue(int limit) {
        int handled = 0;
        while (handled < limit && relayNext()) {
            handled++;
        }
        return handled;
    }

    /** Claims, delivers and marks the next due row. False when no row was due. */
    boolean relayNext() {
        AtomicReference<OutboxEntry> claimed = new AtomicReference<>();
        try {
            transaction.executeWithoutResult(status -> store.claimNextDue().ifPresent(entry -> {
                claimed.set(entry);
                deliver(entry);
                store.markPublished(entry.id());
            }));
        } catch (RuntimeException failure) {
            recordFailure(claimed.get(), failure);
        }
        return claimed.get() != null;
    }

    private void deliver(OutboxEntry entry) {
        IntegrationEvent event = json.read(entry.eventType(), entry.payload());
        ResolvableType relayedType = ResolvableType.forClassWithGenerics(RelayedIE.class, event.getClass());
        listeners.publishEvent(new PayloadApplicationEvent<>(this, new OutboxDelivery<>(event), relayedType));
    }

    private void recordFailure(OutboxEntry entry, RuntimeException failure) {
        if (entry == null) {
            throw failure;
        }
        LOG.warn("Delivery of {} {} failed; it will be retried", entry.eventType(), entry.eventId(), failure);
        transaction.executeWithoutResult(status -> store.recordFailure(entry.id(), String.valueOf(failure)));
    }
}
