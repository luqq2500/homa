package ie.events;

import ie.port.ReservationIE;
import java.time.Instant;
import java.util.Objects;

/** Before the booked start, a named person lapsed or was removed and came off this job's team. When the team is no longer complete, the provider must name someone in their place before check-in. The customer is told. */
public record TeamMemberDroppedIE(
        String reservationId,
        String memberId,
        boolean teamComplete,
        Instant occurredAt) implements ReservationIE {

    public TeamMemberDroppedIE {
        Objects.requireNonNull(reservationId, "reservationId");
        Objects.requireNonNull(memberId, "memberId");
        Objects.requireNonNull(occurredAt, "occurredAt");
    }
}
