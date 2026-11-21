package com.homa.ie.events;

import com.homa.ie.port.ReservationIE;
import com.homa.kernel.Money;
import java.time.Instant;

/**
 * The reservation is confirmed, instantly (ACC-1) or by the provider's acceptance (ACC-2). Payment opens the payout
 * now, so reports made before completion already hold it (RPT-6).
 */
public record ReservationReservedIE(
        String eventId,
        String reservationId,
        String customerId,
        String providerId,
        Instant checkInStart,
        Instant jobStart,
        Instant jobEnd,
        Money total,
        Instant protectionWindowEnd,
        Instant requestedAt,
        Instant occurredAt) implements ReservationIE {
}
