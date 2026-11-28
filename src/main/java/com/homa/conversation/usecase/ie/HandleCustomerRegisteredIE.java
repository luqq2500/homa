package com.homa.conversation.usecase.ie;

import com.homa.conversation.domain.CustomerId;
import com.homa.conversation.domain.CustomerStanding;
import com.homa.conversation.usecase.port.in.CustomerRegisteredIEHandler;
import com.homa.conversation.usecase.port.out.CustomerStandingRepository;
import com.homa.conversation.usecase.port.out.ProcessedMessageStore;
import com.homa.ie.events.CustomerRegisteredIE;

/**
 * CUS-1, CHT-1: a customer registered, so they are in good standing and can start enquiries at once, though not yet
 * identity verified (CUS-3). Customer's events about one customer arrive in order, so the registration is heard
 * first; if a verification was somehow heard before it, the standing it created is kept as it is. Publishes nothing.
 */
public final class HandleCustomerRegisteredIE implements CustomerRegisteredIEHandler {

    /** Stable: never derived from the class name. */
    static final String HANDLER = "conversation.CustomerRegistered";

    private final ProcessedMessageStore processedMessages;
    private final CustomerStandingRepository customers;

    public HandleCustomerRegisteredIE(ProcessedMessageStore processedMessages, CustomerStandingRepository customers) {
        this.processedMessages = processedMessages;
        this.customers = customers;
    }

    @Override
    public void handle(CustomerRegisteredIE event) {
        if (!processedMessages.recordFirstHandling(event.eventId(), HANDLER)) {
            return;
        }
        CustomerId customerId = new CustomerId(event.customerId());
        if (customers.findById(customerId).isEmpty()) {
            customers.save(CustomerStanding.registered(customerId));
        }
    }
}
