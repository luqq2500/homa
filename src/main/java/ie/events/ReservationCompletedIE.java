package ie.events;

import ie.port.ReservationIE;
import java.time.Instant;
import java.util.Objects;

/** A reservation reached Completed. */
public record ReservationCompletedIE(
        String reservationId,
        String ending,
        Instant occurredAt) implements ReservationIE {

    public ReservationCompletedIE {
        Objects.requireNonNull(reservationId, "reservationId");
        Objects.requireNonNull(ending, "ending");
        Objects.requireNonNull(occurredAt, "occurredAt");
    }
}
