package ie.events;

import ie.port.AccountIE;
import java.time.Instant;
import java.util.Objects;

/** A team member passed re-verification, or a lapsed helper passed and rejoined the team. */
public record HelperReverifiedIE(
        String helperId,
        String providerId,
        Instant occurredAt) implements AccountIE {

    public HelperReverifiedIE {
        Objects.requireNonNull(helperId, "helperId");
        Objects.requireNonNull(providerId, "providerId");
        Objects.requireNonNull(occurredAt, "occurredAt");
    }
}
