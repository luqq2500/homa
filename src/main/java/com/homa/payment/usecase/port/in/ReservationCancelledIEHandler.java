package com.homa.payment.usecase.port.in;

import com.homa.ie.events.ReservationCancelledIE;

/** A Reserved reservation was cancelled: Payment executes the settlement lines Reservation worked out (PAY-4 to PAY-7). */
public interface ReservationCancelledIEHandler {

    void handle(ReservationCancelledIE event);
}
