package ie.events;

import ie.port.OfferingIE;
import java.time.Instant;
import java.util.Objects;

/** A provider no longer declares any insurance; the Insured badge comes off. */
public record ProviderInsuranceClearedIE(
        String listingId,
        String providerId,
        Instant occurredAt) implements OfferingIE {

    public ProviderInsuranceClearedIE {
        Objects.requireNonNull(listingId, "listingId");
        Objects.requireNonNull(providerId, "providerId");
        Objects.requireNonNull(occurredAt, "occurredAt");
    }
}
