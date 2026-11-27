package com.homa.reservation.usecase.ie;

import com.homa.ie.events.CustomerRegisteredIE;
import com.homa.reservation.domain.ServiceAddress;
import com.homa.reservation.domain.replica.CustomerStanding;
import com.homa.reservation.usecase.port.in.CustomerRegisteredIEHandler;
import com.homa.reservation.usecase.port.out.CustomerStandingRepository;
import com.homa.reservation.usecase.port.out.ProcessedMessageStore;

/**
 * CUS-1, CUS-3: a customer registered. They can browse at once, but reserve only once Customer says their identity
 * is verified. Their home address, city and arrival note are kept whole, so a reservation can keep them from when it
 * is Requested (RES-8).
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
        customers.save(CustomerStanding.registered(event.customerId(),
                new ServiceAddress(event.streetAddress(), event.city(), event.arrivalNote())));
    }
}
