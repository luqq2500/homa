package com.homa.ie.events;

import com.homa.ie.port.ReservationIE;
import java.time.Instant;

/** A payment landed after the hold expired and a booking rule no longer passes: full refund at once (RES-12). */
public record ReservationCheckoutRejectedIE(
        String eventId,
        String checkoutId, String customerId, String reason, Instant occurredAt)
        implements ReservationIE {
}
