package ie.events;

import ie.port.AccountIE;
import java.time.Instant;
import java.util.Objects;

/** A provider passed re-verification, or a lapsed provider passed and can take new requests again. A job must end by the new {@code reverificationDueAt}. */
public record ProviderReverifiedIE(
        String providerId,
        Instant reverificationDueAt,
        Instant occurredAt) implements AccountIE {

    public ProviderReverifiedIE {
        Objects.requireNonNull(providerId, "providerId");
        Objects.requireNonNull(reverificationDueAt, "reverificationDueAt");
        Objects.requireNonNull(occurredAt, "occurredAt");
    }
}
