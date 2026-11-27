package com.homa.payment.usecase.port.in;

import com.homa.ie.events.ReservationReservedIE;

/** A reservation is Reserved (ACC-1, ACC-2): its payout opens (RPT-6). */
public interface ReservationReservedIEHandler {

    void handle(ReservationReservedIE event);
}
