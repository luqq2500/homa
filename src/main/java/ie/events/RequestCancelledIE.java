package ie.events;

import ie.port.ReservationIE;
import java.time.Instant;
import java.util.Objects;

/** A customer cancelled a request before the provider answered. Nothing is owed: the card authorisation is released, or the upfront payment is refunded in full, as on a decline. */
public record RequestCancelledIE(
        String reservationId,
        String providerId,
        Instant occurredAt) implements ReservationIE {

    public RequestCancelledIE {
        Objects.requireNonNull(reservationId, "reservationId");
        Objects.requireNonNull(providerId, "providerId");
        Objects.requireNonNull(occurredAt, "occurredAt");
    }
}
