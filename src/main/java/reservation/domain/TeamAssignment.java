package reservation.domain;

import java.util.Set;
import reservation.domain.exception.LeadNotInTeamException;

/**
 * Who is coming: the helpers named for this reservation, and the lead, the person in charge on site, who
 * is one of them. A provider who is their own helper can be the lead.
 */
public record TeamAssignment(String lead, Set<String> helpers) {

    public TeamAssignment {
        lead = ReservationText.require(lead, "A lead");
        helpers = Set.copyOf(helpers);
        if (!helpers.contains(lead)) {
            throw new LeadNotInTeamException(lead);
        }
    }

    public static TeamAssignment of(String lead, String... helpers) {
        return new TeamAssignment(lead, Set.of(helpers));
    }
}
