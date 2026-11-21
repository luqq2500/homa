package com.homa.ie.events;

import com.homa.ie.port.ReservationIE;
import com.homa.kernel.Money;
import java.time.Instant;

/**
 * The customer's payment is confirmed and held, and the reservation is Requested (RES-7, PAY-1). Both schedules are
 * blocked. Owners of Homa's reservation values read the version in force at {@code requestedAt} (P3, §11).
 */
public record ReservationRequestedIE(
        String eventId,
        String reservationId,
        String checkoutId,
        String customerId,
        String providerId,
        Instant checkInStart,
        Instant jobStart,
        Instant jobEnd,
        Money total,
        Instant acceptanceDeadline,
        Instant protectionWindowEnd,
        Instant requestedAt,
        Instant occurredAt) implements ReservationIE {
}
