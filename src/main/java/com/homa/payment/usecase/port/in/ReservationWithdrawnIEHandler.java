package com.homa.payment.usecase.port.in;

import com.homa.ie.events.ReservationWithdrawnIE;

/** The customer withdrew a Requested reservation: the payment is refunded in full at once (REQ-2, PAY-7). */
public interface ReservationWithdrawnIEHandler {

    void handle(ReservationWithdrawnIE event);
}
