package ie.events;

import ie.port.ReservationIE;
import java.time.Instant;
import java.util.Objects;

/** The provider declined a request. */
public record ReservationDeclinedIE(
        String reservationId,
        Instant occurredAt) implements ReservationIE {

    public ReservationDeclinedIE {
        Objects.requireNonNull(reservationId, "reservationId");
        Objects.requireNonNull(occurredAt, "occurredAt");
    }
}
