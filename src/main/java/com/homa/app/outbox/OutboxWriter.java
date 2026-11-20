package com.homa.app.outbox;

import com.homa.ie.port.IntegrationEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Writes every integration event a context publishes into the outbox, in the publisher's own transaction.
 *
 * <p>A context's IE publisher in {@code infra/event} only calls {@code ApplicationEventPublisher.publishEvent(event)},
 * so no context depends on {@code com.homa.app}. Spring calls this listener synchronously, on the publisher's thread
 * and inside its transaction: the row commits or rolls back with the aggregate change. Publishing outside a
 * transaction fails ({@link Propagation#MANDATORY}), so an event can never be recorded without the change it reports.
 * A {@link com.homa.ie.port.RelayedIE} is not an {@link IntegrationEvent}, so a relayed delivery is never written back.
 */
@Component
public class OutboxWriter {

    private final OutboxStore store;
    private final IntegrationEventJson json;

    public OutboxWriter(OutboxStore store, IntegrationEventJson json) {
        this.store = store;
        this.json = json;
    }

    @EventListener
    @Transactional(propagation = Propagation.MANDATORY)
    public void record(IntegrationEvent event) {
        store.append(event, json.typeOf(event), json.write(event));
    }
}
