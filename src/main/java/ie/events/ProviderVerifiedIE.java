package ie.events;

import ie.port.AccountIE;
import java.time.Instant;
import java.util.Objects;

/** A provider's eKYC came back verified. A job must end by {@code reverificationDueAt}. */
public record ProviderVerifiedIE(
        String providerId,
        Instant reverificationDueAt,
        Instant occurredAt) implements AccountIE {

    public ProviderVerifiedIE {
        Objects.requireNonNull(providerId, "providerId");
        Objects.requireNonNull(reverificationDueAt, "reverificationDueAt");
        Objects.requireNonNull(occurredAt, "occurredAt");
    }
}
