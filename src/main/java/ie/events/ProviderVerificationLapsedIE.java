package ie.events;

import ie.port.AccountIE;
import java.time.Instant;
import java.util.Objects;

/** The KYC provider reports that a business missed its re-verification; its listing takes no new requests until it passes. */
public record ProviderVerificationLapsedIE(
        String providerId,
        Instant occurredAt) implements AccountIE {

    public ProviderVerificationLapsedIE {
        Objects.requireNonNull(providerId, "providerId");
        Objects.requireNonNull(occurredAt, "occurredAt");
    }
}
