package ie.events;

import ie.port.ReservationIE;
import java.time.Instant;
import java.util.Set;

/** The provider named, or changed, who is coming. */
public record JobTeamNamedIE(String reservationId, String customerId, String providerId, Set<String> teamMemberIds, Instant occurredAt) implements ReservationIE {

    public JobTeamNamedIE {
        teamMemberIds = Set.copyOf(teamMemberIds);
    }
}
