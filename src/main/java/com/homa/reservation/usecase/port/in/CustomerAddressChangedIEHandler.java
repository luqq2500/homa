package com.homa.reservation.usecase.port.in;

import com.homa.ie.events.CustomerAddressChangedIE;

/** RES-1, RES-8: the customer's home address for new bookings. */
public interface CustomerAddressChangedIEHandler {

    void handle(CustomerAddressChangedIE event);
}
