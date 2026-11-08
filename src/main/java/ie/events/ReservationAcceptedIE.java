package ie.events;

import ie.port.ReservationIE;
import java.time.Instant;
import java.util.Objects;

/** The provider accepted a request; the slot is held and a card is charged. */
public record ReservationAcceptedIE(
        String reservationId,
        String providerId,
        Instant start,
        Instant end,
        Instant occurredAt) implements ReservationIE {

    public ReservationAcceptedIE {
        Objects.requireNonNull(reservationId, "reservationId");
        Objects.requireNonNull(providerId, "providerId");
        Objects.requireNonNull(start, "start");
        Objects.requireNonNull(end, "end");
        Objects.requireNonNull(occurredAt, "occurredAt");
    }
}
