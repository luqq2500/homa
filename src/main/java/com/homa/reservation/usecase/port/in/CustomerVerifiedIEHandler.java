package com.homa.reservation.usecase.port.in;

import com.homa.ie.events.CustomerVerifiedIE;

/** CUS-3: the customer can reserve. */
public interface CustomerVerifiedIEHandler {

    void handle(CustomerVerifiedIE event);
}
