package com.homa.payment.usecase.port.in;

import com.homa.ie.events.ReservationDeclinedIE;

/** The provider declined a Requested reservation: the payment is refunded in full at once (REQ-3, PAY-7). */
public interface ReservationDeclinedIEHandler {

    void handle(ReservationDeclinedIE event);
}
