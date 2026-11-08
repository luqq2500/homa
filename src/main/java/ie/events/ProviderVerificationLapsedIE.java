package ie.events;

import ie.port.AccountIE;
import java.time.Instant;
import java.util.Objects;

/** A provider missed their re-verification due date; their listing takes no new requests until they pass. */
public record ProviderVerificationLapsedIE(
        String providerId,
        Instant occurredAt) implements AccountIE {

    public ProviderVerificationLapsedIE {
        Objects.requireNonNull(providerId, "providerId");
        Objects.requireNonNull(occurredAt, "occurredAt");
    }
}
