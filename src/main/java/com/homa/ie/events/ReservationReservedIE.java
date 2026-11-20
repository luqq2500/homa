package com.homa.ie.events;

import com.homa.ie.port.ReservationIE;
import com.homa.kernel.Money;
import java.time.Instant;

/** The reservation is confirmed, instantly (ACC-1) or by the provider's acceptance (ACC-2). */
public record ReservationReservedIE(
        String eventId,
        String reservationId,
        String customerId,
        String providerId,
        Instant jobStart,
        Instant jobEnd,
        Money total,
        Instant protectionWindowEnd,
        Instant occurredAt) implements ReservationIE {
}
