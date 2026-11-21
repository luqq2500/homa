package com.homa.ie.events;

import com.homa.ie.port.PaymentIE;
import java.time.Instant;

/** The gateway refused a payment attempt. Nothing was blocked, so there is nothing to release; the customer can retry (RES-11). */
public record PaymentFailedIE(
        String eventId,
        String checkoutId, String reason, Instant occurredAt) implements PaymentIE {
}
