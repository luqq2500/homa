package ie.events;

import ie.port.AccountIE;
import java.time.Instant;
import java.util.Objects;

/** A provider's eKYC, or a business's eKYB, came back rejected; the provider must redo it. */
public record ProviderVerificationRejectedIE(
        String providerId,
        Instant occurredAt) implements AccountIE {

    public ProviderVerificationRejectedIE {
        Objects.requireNonNull(providerId, "providerId");
        Objects.requireNonNull(occurredAt, "occurredAt");
    }
}
