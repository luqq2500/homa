package ie.events;

import ie.port.ReservationIE;
import java.time.Instant;
import java.util.Objects;

/** Check-in completed: the first face check passed, or the face check service was unavailable and check-in went ahead on location only. */
public record CheckInCompletedIE(
        String reservationId,
        Instant checkedInAt,
        boolean faceCheckUnavailable,
        Instant occurredAt) implements ReservationIE {

    public CheckInCompletedIE {
        Objects.requireNonNull(reservationId, "reservationId");
        Objects.requireNonNull(checkedInAt, "checkedInAt");
        Objects.requireNonNull(occurredAt, "occurredAt");
    }
}
