package ie.events;

import ie.port.ReservationIE;
import java.time.Instant;
import java.util.Objects;
import java.util.Set;

/** The provider named who is coming: the helpers, and any supervisors. */
public record TeamNamedIE(
        String reservationId,
        Set<String> helperIds,
        Set<String> supervisorIds,
        Instant occurredAt) implements ReservationIE {

    public TeamNamedIE {
        Objects.requireNonNull(reservationId, "reservationId");
        helperIds = Set.copyOf(helperIds);
        supervisorIds = Set.copyOf(supervisorIds);
        Objects.requireNonNull(occurredAt, "occurredAt");
    }
}
