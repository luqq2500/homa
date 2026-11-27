package com.homa.reservation.usecase.ie;

import com.homa.ie.events.PaymentFailedIE;
import com.homa.reservation.usecase.port.in.PaymentFailedIEHandler;
import com.homa.reservation.usecase.port.out.ProcessedMessageStore;

/**
 * RES-11: a payment attempt failed. No time is blocked until a payment is confirmed, so there is nothing to release;
 * the checkout stays and the customer can retry. Recording the event is all there is to do.
 */
public final class HandlePaymentFailedIE implements PaymentFailedIEHandler {

    static final String HANDLER = "reservation.PaymentFailed";

    private final ProcessedMessageStore processed;

    public HandlePaymentFailedIE(ProcessedMessageStore processed) {
        this.processed = processed;
    }

    @Override
    public void handle(PaymentFailedIE event) {
        processed.recordFirstHandling(event.eventId(), HANDLER);
    }
}
