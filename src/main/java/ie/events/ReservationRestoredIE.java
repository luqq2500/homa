package ie.events;

import ie.port.ReservationIE;
import java.time.Instant;
import java.util.Objects;

/** The KYC provider cleared a provider's flag, and Homa restored a disputed reservation before its booked start; it goes ahead as booked. */
public record ReservationRestoredIE(
        String reservationId,
        String providerId,
        Instant occurredAt) implements ReservationIE {

    public ReservationRestoredIE {
        Objects.requireNonNull(reservationId, "reservationId");
        Objects.requireNonNull(providerId, "providerId");
        Objects.requireNonNull(occurredAt, "occurredAt");
    }
}
