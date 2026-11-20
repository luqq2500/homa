package com.homa.app.outbox.sample.conversation.usecase.ie;

import com.homa.app.outbox.sample.conversation.usecase.port.in.CustomerRegisteredIEHandler;
import com.homa.app.outbox.sample.conversation.usecase.port.out.Members;
import com.homa.app.outbox.sample.conversation.usecase.port.out.ProcessedMessageStore;
import com.homa.ie.events.CustomerRegisteredIE;

public class HandleCustomerRegisteredIE implements CustomerRegisteredIEHandler {

    static final String HANDLER = "conversation.CustomerRegistered";

    private final ProcessedMessageStore processed;
    private final Members members;

    public HandleCustomerRegisteredIE(ProcessedMessageStore processed, Members members) {
        this.processed = processed;
        this.members = members;
    }

    @Override
    public void handle(CustomerRegisteredIE event) {
        if (!processed.recordFirstHandling(event.eventId(), HANDLER)) {
            return;
        }
        members.admit(event.customerId());
    }
}
