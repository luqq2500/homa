package com.homa.reservation.usecase.ie;

import com.homa.ie.events.CustomerRegisteredIE;
import com.homa.reservation.domain.replica.CustomerStanding;
import com.homa.reservation.usecase.port.in.CustomerRegisteredIEHandler;
import com.homa.reservation.usecase.port.out.CustomerStandingRepository;
import com.homa.reservation.usecase.port.out.ProcessedMessageStore;

/**
 * CUS-1, CUS-3: a customer registered. They can browse at once, but reserve only once Customer says their identity
 * is verified.
 */
public final class HandleCustomerRegisteredIE implements CustomerRegisteredIEHandler {

    static final String HANDLER = "reservation.CustomerRegistered";

    private final ProcessedMessageStore processed;
    private final CustomerStandingRepository customers;

    public HandleCustomerRegisteredIE(ProcessedMessageStore processed, CustomerStandingRepository customers) {
        this.processed = processed;
        this.customers = customers;
    }

    @Override
    public void handle(CustomerRegisteredIE event) {
        if (!processed.recordFirstHandling(event.eventId(), HANDLER)) {
            return;
        }
        customers.save(CustomerStanding.registered(event.customerId(), CustomerHomeAddress.inCity(event.city())));
    }
}
