package ie.events;

import ie.port.AccountIE;
import java.time.Instant;
import java.util.Objects;

/** A provider's eKYC came back verified. */
public record ProviderVerifiedIE(
        String providerId,
        Instant occurredAt) implements AccountIE {

    public ProviderVerifiedIE {
        Objects.requireNonNull(providerId, "providerId");
        Objects.requireNonNull(occurredAt, "occurredAt");
    }
}
