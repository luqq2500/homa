package com.homa.ie.events;

import com.homa.ie.port.ReservationIE;
import java.time.Instant;

/**
 * The customer withdrew a Requested reservation before the provider accepted it (REQ-2). Payment refunds in full at
 * once (PAY-7) and the time opens again. A withdrawal is not a cancellation: Rating does not consume this event, so
 * it never counts in reliability (RTG-10).
 */
public record ReservationWithdrawnIE(
        String eventId,
        String reservationId,
        String customerId,
        String providerId,
        Instant withdrawnAt,
        Instant requestedAt,
        Instant occurredAt) implements ReservationIE {
}
