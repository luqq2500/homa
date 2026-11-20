package com.homa.app.outbox.sample.registration.infra.event;

import com.homa.app.outbox.sample.DeliveryLog;
import com.homa.ie.events.CustomerRegisteredIE;
import org.springframework.context.event.EventListener;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Same-context handling inside the publisher's transaction: allowed only for the publishing context's own events,
 * so it listens to the bare event. The relay's RelayedIE never reaches it.
 */
public class SpringRegistrationIEListener {

    private final DeliveryLog log;

    public SpringRegistrationIEListener(DeliveryLog log) {
        this.log = log;
    }

    @EventListener
    public void on(CustomerRegisteredIE event) {
        log.inPublisherTransaction(event, TransactionSynchronizationManager.isActualTransactionActive());
    }
}
