package ie.events;

import ie.port.AccountIE;
import java.time.Instant;
import java.util.Objects;

/** The KYC provider re-verified a business, or a lapsed one passed and can take new requests again. A job must end by the new {@code verifiedUntil}. */
public record ProviderReverifiedIE(
        String providerId,
        Instant verifiedUntil,
        Instant occurredAt) implements AccountIE {

    public ProviderReverifiedIE {
        Objects.requireNonNull(providerId, "providerId");
        Objects.requireNonNull(verifiedUntil, "verifiedUntil");
        Objects.requireNonNull(occurredAt, "occurredAt");
    }
}
