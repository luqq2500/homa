package com.homa.ie.events;

import com.homa.ie.port.ReservationIE;
import com.homa.kernel.Money;
import java.time.Instant;

/** A customer started paying; the provider's time is held for 15 minutes (RES-11). Payment opens a pending hold. */
public record ReservationCheckoutStartedIE(
        String checkoutId, String customerId, String providerId, Money total, Instant holdExpiresAt, Instant occurredAt)
        implements ReservationIE {
}
