package com.homa.reservation.usecase.ie;

import com.homa.ie.events.CustomerAddressChangedIE;
import com.homa.reservation.usecase.port.in.CustomerAddressChangedIEHandler;
import com.homa.reservation.usecase.port.out.CustomerStandingRepository;
import com.homa.reservation.usecase.port.out.ProcessedMessageStore;

/**
 * RES-1, RES-8: the customer moved. New bookings use the new home address; booked reservations keep the address
 * they were Requested with.
 */
public final class HandleCustomerAddressChangedIE implements CustomerAddressChangedIEHandler {

    static final String HANDLER = "reservation.CustomerAddressChanged";

    private final ProcessedMessageStore processed;
    private final CustomerStandingRepository customers;

    public HandleCustomerAddressChangedIE(ProcessedMessageStore processed, CustomerStandingRepository customers) {
        this.processed = processed;
        this.customers = customers;
    }

    @Override
    public void handle(CustomerAddressChangedIE event) {
        if (!processed.recordFirstHandling(event.eventId(), HANDLER)) {
            return;
        }
        customers.findById(event.customerId())
                .map(customer -> customer.movedTo(CustomerHomeAddress.inCity(event.city())))
                .ifPresent(customers::save);
    }
}
