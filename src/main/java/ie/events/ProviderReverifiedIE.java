package ie.events;

import ie.port.AccountIE;
import java.time.Instant;
import java.util.Objects;

/** A provider passed re-verification, or a lapsed provider passed and can take new requests again. */
public record ProviderReverifiedIE(
        String providerId,
        Instant occurredAt) implements AccountIE {

    public ProviderReverifiedIE {
        Objects.requireNonNull(providerId, "providerId");
        Objects.requireNonNull(occurredAt, "occurredAt");
    }
}
