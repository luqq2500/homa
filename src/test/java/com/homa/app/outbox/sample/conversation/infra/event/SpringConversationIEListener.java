package com.homa.app.outbox.sample.conversation.infra.event;

import com.homa.app.outbox.sample.DeliveryLog;
import com.homa.app.outbox.sample.conversation.usecase.port.in.CustomerRegisteredIEHandler;
import com.homa.ie.events.CustomerRegisteredIE;
import com.homa.ie.events.PaymentHeldIE;
import com.homa.ie.port.RelayedIE;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

public class SpringConversationIEListener {

    private final CustomerRegisteredIEHandler handler;
    private final DeliveryLog log;

    public SpringConversationIEListener(CustomerRegisteredIEHandler handler, DeliveryLog log) {
        this.handler = handler;
        this.log = log;
    }

    @EventListener
    @Order(2)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onCustomerRegistered(RelayedIE<CustomerRegisteredIE> relayed) {
        log.relayed("conversation", relayed.event());
        handler.handle(relayed.event());
    }

    /** Must see only PaymentHeldIE deliveries: the relay matches a delivery to its event type, not just to RelayedIE. */
    @EventListener
    public void onPaymentHeld(RelayedIE<PaymentHeldIE> relayed) {
        log.relayedOtherType("conversation", relayed.event().eventId());
    }
}
