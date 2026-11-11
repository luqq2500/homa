package ie.events;

import ie.port.AccountIE;
import java.time.Instant;
import java.util.Objects;

/** The KYC provider verified a business through KYB, with its owner's or director's KYC. A job must end by {@code verifiedUntil}. */
public record ProviderVerifiedIE(
        String providerId,
        String ssmNumber,
        Instant verifiedUntil,
        Instant occurredAt) implements AccountIE {

    public ProviderVerifiedIE {
        Objects.requireNonNull(providerId, "providerId");
        Objects.requireNonNull(ssmNumber, "ssmNumber");
        Objects.requireNonNull(verifiedUntil, "verifiedUntil");
        Objects.requireNonNull(occurredAt, "occurredAt");
    }
}
