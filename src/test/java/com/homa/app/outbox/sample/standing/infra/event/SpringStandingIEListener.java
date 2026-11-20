package com.homa.app.outbox.sample.standing.infra.event;

import com.homa.app.outbox.sample.DeliveryLog;
import com.homa.app.outbox.sample.standing.usecase.port.in.CustomerRegisteredIEHandler;
import com.homa.ie.events.CustomerRegisteredIE;
import com.homa.ie.port.RelayedIE;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * The pattern for a cross-context listener in infra/event: listen for the relayed event, synchronously, and pass
 * it to the handler interface in the handler's own transaction. If it throws, the relay keeps the row and retries.
 */
public class SpringStandingIEListener {

    private final CustomerRegisteredIEHandler handler;
    private final DeliveryLog log;

    public SpringStandingIEListener(CustomerRegisteredIEHandler handler, DeliveryLog log) {
        this.handler = handler;
        this.log = log;
    }

    @EventListener
    @Order(1)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onCustomerRegistered(RelayedIE<CustomerRegisteredIE> relayed) {
        log.relayed("standing", relayed.event());
        handler.handle(relayed.event());
    }
}
