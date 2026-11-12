package ie.events;

import ie.port.OfferingIE;
import java.time.Instant;
import java.util.Objects;

/** A provider declared their liability insurance; the provider page shows the Insured badge until it expires. */
public record ProviderInsuranceDeclaredIE(
        String listingId,
        String providerId,
        String insurer,
        Instant validUntil,
        Instant occurredAt) implements OfferingIE {

    public ProviderInsuranceDeclaredIE {
        Objects.requireNonNull(listingId, "listingId");
        Objects.requireNonNull(providerId, "providerId");
        Objects.requireNonNull(insurer, "insurer");
        Objects.requireNonNull(validUntil, "validUntil");
        Objects.requireNonNull(occurredAt, "occurredAt");
    }
}
