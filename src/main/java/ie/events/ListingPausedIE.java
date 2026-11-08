package ie.events;

import ie.port.OfferingIE;
import java.time.Instant;
import java.util.Objects;

/** A live listing was paused because the provider's re-verification lapsed; it stays live but takes no new requests, and search stops showing it. */
public record ListingPausedIE(
        String listingId,
        String providerId,
        Instant occurredAt) implements OfferingIE {

    public ListingPausedIE {
        Objects.requireNonNull(listingId, "listingId");
        Objects.requireNonNull(providerId, "providerId");
        Objects.requireNonNull(occurredAt, "occurredAt");
    }
}
