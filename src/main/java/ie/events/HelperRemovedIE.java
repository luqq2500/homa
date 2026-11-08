package ie.events;

import ie.port.AccountIE;
import java.time.Instant;
import java.util.Objects;

/** A provider withdrew a helper's invite or removed a team member. */
public record HelperRemovedIE(
        String helperId,
        String providerId,
        Instant occurredAt) implements AccountIE {

    public HelperRemovedIE {
        Objects.requireNonNull(helperId, "helperId");
        Objects.requireNonNull(providerId, "providerId");
        Objects.requireNonNull(occurredAt, "occurredAt");
    }
}
