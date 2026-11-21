package com.homa.ie.events;

import com.homa.ie.port.ReservationIE;
import com.homa.kernel.Money;
import java.time.Instant;

/**
 * A customer started paying. The time was free when checked, but nothing is held: the first confirmed payment for a
 * time wins (P1, RES-11). Payment opens a pending hold, which may see several payment attempts.
 */
public record ReservationCheckoutStartedIE(
        String eventId,
        String checkoutId, String customerId, String providerId, Money total, Instant occurredAt)
        implements ReservationIE {
}
