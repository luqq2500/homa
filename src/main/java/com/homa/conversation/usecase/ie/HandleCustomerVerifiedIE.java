package com.homa.conversation.usecase.ie;

import com.homa.conversation.domain.CustomerId;
import com.homa.conversation.domain.CustomerStanding;
import com.homa.conversation.usecase.port.in.CustomerVerifiedIEHandler;
import com.homa.conversation.usecase.port.out.CustomerStandingRepository;
import com.homa.conversation.usecase.port.out.ProcessedMessageStore;
import com.homa.ie.events.CustomerVerifiedIE;

/**
 * CUS-3: Customer says the customer's identity is verified. Enquiries never needed it, so the fact changes nothing
 * about what the customer can do here; it is kept with their standing. If the verification is heard before the
 * registration, the standing is created from it, and the registration then leaves it as it is, so the fact is never
 * lost. Publishes nothing.
 */
public final class HandleCustomerVerifiedIE implements CustomerVerifiedIEHandler {

    /** Stable: never derived from the class name. */
    static final String HANDLER = "conversation.CustomerVerified";

    private final ProcessedMessageStore processedMessages;
    private final CustomerStandingRepository customers;

    public HandleCustomerVerifiedIE(ProcessedMessageStore processedMessages, CustomerStandingRepository customers) {
        this.processedMessages = processedMessages;
        this.customers = customers;
    }

    @Override
    public void handle(CustomerVerifiedIE event) {
        if (!processedMessages.recordFirstHandling(event.eventId(), HANDLER)) {
            return;
        }
        CustomerId customerId = new CustomerId(event.customerId());
        customers.save(customers.findById(customerId)
                .orElseGet(() -> CustomerStanding.registered(customerId))
                .verified());
    }
}
