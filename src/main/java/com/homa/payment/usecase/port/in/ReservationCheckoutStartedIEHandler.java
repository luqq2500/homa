package com.homa.payment.usecase.port.in;

import com.homa.ie.events.ReservationCheckoutStartedIE;

/** A customer started a checkout (RES-11). */
public interface ReservationCheckoutStartedIEHandler {

    void handle(ReservationCheckoutStartedIE event);
}
