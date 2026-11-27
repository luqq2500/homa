package com.homa.payment.usecase.port.in;

import com.homa.ie.events.ReservationCheckoutRejectedIE;

/** A confirmed payment could not become a reservation (RES-11). */
public interface ReservationCheckoutRejectedIEHandler {

    void handle(ReservationCheckoutRejectedIE event);
}
