package ie.events;

import ie.port.ReservationIE;
import java.time.Instant;
import java.util.Objects;

/** The lead checked in at the customer's location. */
public record ProviderCheckedInIE(
        String reservationId,
        Instant checkedInAt,
        int distanceMetres,
        Instant occurredAt) implements ReservationIE {

    public ProviderCheckedInIE {
        Objects.requireNonNull(reservationId, "reservationId");
        Objects.requireNonNull(checkedInAt, "checkedInAt");
        Objects.requireNonNull(occurredAt, "occurredAt");
    }
}
