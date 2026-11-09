package ie.events;

import ie.port.AccountIE;
import java.time.Instant;
import java.util.Objects;

/** A provider registered and chose their type, INDEPENDENT or BUSINESS. */
public record ProviderRegisteredIE(
        String providerId,
        String providerType,
        Instant occurredAt) implements AccountIE {

    public ProviderRegisteredIE {
        Objects.requireNonNull(providerId, "providerId");
        Objects.requireNonNull(providerType, "providerType");
        Objects.requireNonNull(occurredAt, "occurredAt");
    }
}
