package com.homa.reservation.usecase.ie;

import com.homa.ie.events.CustomerVerifiedIE;
import com.homa.reservation.domain.replica.CustomerStanding;
import com.homa.reservation.usecase.port.in.CustomerVerifiedIEHandler;
import com.homa.reservation.usecase.port.out.CustomerStandingRepository;
import com.homa.reservation.usecase.port.out.ProcessedMessageStore;

/**
 * CUS-3, CUS-10: Customer says the customer's identity is verified, so they can reserve. Customer's events reach
 * Reservation in order, so the registration is always known first.
 */
public final class HandleCustomerVerifiedIE implements CustomerVerifiedIEHandler {

    static final String HANDLER = "reservation.CustomerVerified";

    private final ProcessedMessageStore processed;
    private final CustomerStandingRepository customers;

    public HandleCustomerVerifiedIE(ProcessedMessageStore processed, CustomerStandingRepository customers) {
        this.processed = processed;
        this.customers = customers;
    }

    @Override
    public void handle(CustomerVerifiedIE event) {
        if (!processed.recordFirstHandling(event.eventId(), HANDLER)) {
            return;
        }
        customers.findById(event.customerId()).map(CustomerStanding::verified).ifPresent(customers::save);
    }
}
