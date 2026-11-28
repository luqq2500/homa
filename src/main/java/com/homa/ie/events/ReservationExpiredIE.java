package com.homa.ie.events;

import com.homa.ie.port.ReservationIE;
import java.time.Instant;

/**
 * A Requested reservation passed its acceptance deadline without an answer (REQ-4, ACC-3). Payment refunds in full
 * at once (PAY-7); Conversation turns the chat read-only (CHT-5); the time opens again (RES-9). Rating does not
 * consume it (RTG-10).
 */
public record ReservationExpiredIE(
        String eventId,
        String reservationId,
        String customerId,
        String providerId,
        Instant expiredAt,
        Instant requestedAt,
        Instant occurredAt) implements ReservationIE {
}
