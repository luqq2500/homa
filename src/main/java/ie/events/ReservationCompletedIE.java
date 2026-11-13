package ie.events;

import ie.port.ReservationIE;
import java.time.Instant;
import java.util.Set;

/** A reservation reached Completed: the dispute window ended with no report, or Homa resolved the review. */
public record ReservationCompletedIE(String reservationId, String customerId, String providerId, Set<String> teamMemberIds, boolean providerPaid, Instant occurredAt) implements ReservationIE {

    public ReservationCompletedIE {
        teamMemberIds = Set.copyOf(teamMemberIds);
    }
}
