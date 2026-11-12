package ie.events;

import ie.port.ReservationIE;
import java.time.Instant;
import java.util.Objects;

/** From the booked start, a named person lapsed or was removed, so they can no longer work on this job. The customer is told. */
public record TeamMemberStoppedIE(
        String reservationId,
        String memberId,
        Instant occurredAt) implements ReservationIE {

    public TeamMemberStoppedIE {
        Objects.requireNonNull(reservationId, "reservationId");
        Objects.requireNonNull(memberId, "memberId");
        Objects.requireNonNull(occurredAt, "occurredAt");
    }
}
