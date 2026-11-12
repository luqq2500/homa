package ie.events;

import ie.port.OfferingIE;
import java.time.Instant;
import java.util.Objects;

/** Homa narrowed its duration range and none of this live listing's hours were inside it, so it is no longer live until the provider sets hours and goes live again. The provider is told. */
public record ListingWentOfflineForHoursIE(
        String listingId,
        String providerId,
        Instant occurredAt) implements OfferingIE {

    public ListingWentOfflineForHoursIE {
        Objects.requireNonNull(listingId, "listingId");
        Objects.requireNonNull(providerId, "providerId");
        Objects.requireNonNull(occurredAt, "occurredAt");
    }
}
