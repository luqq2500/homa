package ie.events;

import ie.port.ReservationIE;
import java.time.Instant;
import java.util.Objects;

/** An accepted reservation of a seriously flagged provider moved to Disputed; Homa cancels it with a full refund. */
public record ReservationFlaggedIE(
        String reservationId,
        String providerId,
        Instant occurredAt) implements ReservationIE {

    public ReservationFlaggedIE {
        Objects.requireNonNull(reservationId, "reservationId");
        Objects.requireNonNull(providerId, "providerId");
        Objects.requireNonNull(occurredAt, "occurredAt");
    }
}
