package ie.events;

import ie.port.ReservationIE;
import java.time.Instant;
import java.util.Objects;

/** A reservation ended cancelled: by the customer, by the provider, or as a no-show. */
public record ReservationCancelledIE(
        String reservationId,
        String providerId,
        String ending,
        Instant occurredAt) implements ReservationIE {

    public ReservationCancelledIE {
        Objects.requireNonNull(reservationId, "reservationId");
        Objects.requireNonNull(providerId, "providerId");
        Objects.requireNonNull(ending, "ending");
        Objects.requireNonNull(occurredAt, "occurredAt");
    }
}
