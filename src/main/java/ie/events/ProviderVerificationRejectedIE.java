package ie.events;

import ie.port.AccountIE;
import java.time.Instant;
import java.util.Objects;

/** The KYC provider rejected a business's KYB; the business must redo it. */
public record ProviderVerificationRejectedIE(
        String providerId,
        Instant occurredAt) implements AccountIE {

    public ProviderVerificationRejectedIE {
        Objects.requireNonNull(providerId, "providerId");
        Objects.requireNonNull(occurredAt, "occurredAt");
    }
}
