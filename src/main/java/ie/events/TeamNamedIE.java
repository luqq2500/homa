package ie.events;

import ie.port.ReservationIE;
import java.time.Instant;
import java.util.Objects;
import java.util.Set;

/** The provider named who is coming. */
public record TeamNamedIE(
        String reservationId,
        String leadId,
        Set<String> helperIds,
        Instant occurredAt) implements ReservationIE {

    public TeamNamedIE {
        Objects.requireNonNull(reservationId, "reservationId");
        Objects.requireNonNull(leadId, "leadId");
        helperIds = Set.copyOf(helperIds);
        Objects.requireNonNull(occurredAt, "occurredAt");
    }
}
