package com.homa.reservation.usecase.port.in;

import com.homa.ie.events.PaymentHeldIE;

/** RES-7, RES-11, ACC-1: a confirmed payment becomes a reservation if its time is still free. */
public interface PaymentHeldIEHandler {

    void handle(PaymentHeldIE event);
}
