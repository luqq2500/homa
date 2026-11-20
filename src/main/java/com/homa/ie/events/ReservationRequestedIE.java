package com.homa.ie.events;

import com.homa.ie.port.ReservationIE;
import com.homa.kernel.Money;
import java.time.Instant;

/** The customer has paid and the payment is held (RES-7, PAY-1). */
public record ReservationRequestedIE(
        String eventId,
        String reservationId,
        String checkoutId,
        String customerId,
        String providerId,
        Instant jobStart,
        Instant jobEnd,
        Money total,
        Instant acceptanceDeadline,
        Instant protectionWindowEnd,
        Instant occurredAt) implements ReservationIE {
}
