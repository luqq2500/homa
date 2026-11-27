package com.homa.reservation.usecase.port.in;

import com.homa.ie.events.CustomerRegisteredIE;

/** CUS-1, CUS-3: a new customer, not yet identity verified. */
public interface CustomerRegisteredIEHandler {

    void handle(CustomerRegisteredIE event);
}
