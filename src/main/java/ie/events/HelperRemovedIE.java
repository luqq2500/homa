package ie.events;

import ie.port.AccountIE;
import java.time.Instant;
import java.util.Objects;

/** A helper was removed from a provider's team. */
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
