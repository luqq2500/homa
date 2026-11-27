package com.homa.reservation.usecase.port.in;

import com.homa.ie.events.PaymentFailedIE;

/** RES-11: a refused payment attempt; nothing was held, so nothing is released. */
public interface PaymentFailedIEHandler {

    void handle(PaymentFailedIE event);
}
