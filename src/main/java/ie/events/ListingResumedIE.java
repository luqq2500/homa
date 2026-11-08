package ie.events;

import ie.port.OfferingIE;
import java.time.Instant;
import java.util.Objects;

/** A paused listing takes new requests again because the provider passed re-verification. */
public record ListingResumedIE(
        String listingId,
        String providerId,
        Instant occurredAt) implements OfferingIE {

    public ListingResumedIE {
        Objects.requireNonNull(listingId, "listingId");
        Objects.requireNonNull(providerId, "providerId");
        Objects.requireNonNull(occurredAt, "occurredAt");
    }
}
