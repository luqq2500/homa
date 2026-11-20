package com.homa.app.outbox.sample.standing.usecase.ie;

import com.homa.app.outbox.sample.standing.usecase.port.in.CustomerRegisteredIEHandler;
import com.homa.app.outbox.sample.standing.usecase.port.out.CustomerStandings;
import com.homa.app.outbox.sample.standing.usecase.port.out.ProcessedMessageStore;
import com.homa.ie.events.CustomerRegisteredIE;

/**
 * The idempotent handler pattern: record the event first, and do nothing on a repeat. The record and the effect
 * share the handler's transaction, so a failed effect also forgets the record and the retry runs it again.
 */
public class HandleCustomerRegisteredIE implements CustomerRegisteredIEHandler {

    /** Stable across renames of the class: changing it would make every old event look new to this handler. */
    static final String HANDLER = "standing.CustomerRegistered";

    private final ProcessedMessageStore processed;
    private final CustomerStandings standings;

    public HandleCustomerRegisteredIE(ProcessedMessageStore processed, CustomerStandings standings) {
        this.processed = processed;
        this.standings = standings;
    }

    @Override
    public void handle(CustomerRegisteredIE event) {
        if (!processed.recordFirstHandling(event.eventId(), HANDLER)) {
            return;
        }
        standings.recordRegistration(event.customerId(), event.city());
    }
}
