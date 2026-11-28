package com.homa.payment.usecase.port.in;

import com.homa.ie.events.ReservationRequestedIE;

/** The reservation is Requested and its payment is held (RES-7, PAY-1). */
public interface ReservationRequestedIEHandler {

    void handle(ReservationRequestedIE event);
}
