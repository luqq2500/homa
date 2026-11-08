package ie.events;

import ie.port.AccountIE;
import java.time.Instant;
import java.util.Objects;

/** A helper's eKYC came back verified. */
public record HelperVerifiedIE(
        String helperId,
        String providerId,
        Instant occurredAt) implements AccountIE {

    public HelperVerifiedIE {
        Objects.requireNonNull(helperId, "helperId");
        Objects.requireNonNull(providerId, "providerId");
        Objects.requireNonNull(occurredAt, "occurredAt");
    }
}
