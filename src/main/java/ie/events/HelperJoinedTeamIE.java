package ie.events;

import ie.port.AccountIE;
import java.time.Instant;
import java.util.Objects;

/** An invited helper's eKYC came back verified; the helper is now a team member. */
public record HelperJoinedTeamIE(
        String helperId,
        String providerId,
        Instant occurredAt) implements AccountIE {

    public HelperJoinedTeamIE {
        Objects.requireNonNull(helperId, "helperId");
        Objects.requireNonNull(providerId, "providerId");
        Objects.requireNonNull(occurredAt, "occurredAt");
    }
}
