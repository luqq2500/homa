package com.homa.ie.events;

import com.homa.ie.port.ReservationIE;
import java.time.Instant;

/**
 * The provider declined a Requested reservation (REQ-3). Payment refunds in full at once (PAY-7); Conversation turns
 * the chat read-only (CHT-5); the time opens again (RES-9). A decline is not a cancellation: no fee, no fine, and
 * Rating does not consume this event, so it never counts in reliability (RTG-10).
 */
public record ReservationDeclinedIE(
        String eventId,
        String reservationId,
        String customerId,
        String providerId,
        Instant declinedAt,
        Instant requestedAt,
        Instant occurredAt) implements ReservationIE {
}
