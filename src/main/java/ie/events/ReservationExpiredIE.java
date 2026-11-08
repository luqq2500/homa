package ie.events;

import ie.port.ReservationIE;
import java.time.Instant;
import java.util.Objects;

/** A request expired without an answer. */
public record ReservationExpiredIE(
        String reservationId,
        Instant occurredAt) implements ReservationIE {

    public ReservationExpiredIE {
        Objects.requireNonNull(reservationId, "reservationId");
        Objects.requireNonNull(occurredAt, "occurredAt");
    }
}
