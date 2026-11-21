package com.homa.ie.events;

import com.homa.ie.port.ReservationIE;
import java.time.Instant;

/**
 * A confirmed payment could not become a reservation: another customer's payment took the time first, or a booking
 * rule no longer passes. Payment refunds it in full at once and the customer is told (P1, RES-11).
 *
 * @param reason TIME_TAKEN or RULE_FAILED
 */
public record ReservationCheckoutRejectedIE(
        String eventId,
        String checkoutId, String customerId, String reason, Instant occurredAt)
        implements ReservationIE {
}
