package com.homa.ie.events;

import com.homa.ie.port.PaymentIE;
import java.time.Instant;

/** The gateway refused the payment; Reservation releases the checkout hold (RES-11). */
public record PaymentFailedIE(
        String eventId,
        String checkoutId, String reason, Instant occurredAt) implements PaymentIE {
}
