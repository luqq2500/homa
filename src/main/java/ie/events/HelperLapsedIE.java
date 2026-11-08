package ie.events;

import ie.port.AccountIE;
import java.time.Instant;
import java.util.Objects;

/** A team member missed their re-verification due date and left the team until they pass. */
public record HelperLapsedIE(
        String helperId,
        String providerId,
        Instant occurredAt) implements AccountIE {

    public HelperLapsedIE {
        Objects.requireNonNull(helperId, "helperId");
        Objects.requireNonNull(providerId, "providerId");
        Objects.requireNonNull(occurredAt, "occurredAt");
    }
}
