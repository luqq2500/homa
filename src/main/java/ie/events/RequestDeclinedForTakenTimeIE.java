package ie.events;

import ie.port.ReservationIE;
import java.time.Instant;
import java.util.Objects;

/** The provider accepted another booking that takes this request's time, so it was declined at once. The customer pays nothing and is told. */
public record RequestDeclinedForTakenTimeIE(
        String reservationId,
        String customerId,
        String providerId,
        Instant occurredAt) implements ReservationIE {

    public RequestDeclinedForTakenTimeIE {
        Objects.requireNonNull(reservationId, "reservationId");
        Objects.requireNonNull(customerId, "customerId");
        Objects.requireNonNull(providerId, "providerId");
        Objects.requireNonNull(occurredAt, "occurredAt");
    }
}
