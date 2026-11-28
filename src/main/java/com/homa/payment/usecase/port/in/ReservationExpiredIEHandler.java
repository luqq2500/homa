package com.homa.payment.usecase.port.in;

import com.homa.ie.events.ReservationExpiredIE;

/** A Requested reservation passed its acceptance deadline: the payment is refunded in full at once (REQ-4, PAY-7). */
public interface ReservationExpiredIEHandler {

    void handle(ReservationExpiredIE event);
}
