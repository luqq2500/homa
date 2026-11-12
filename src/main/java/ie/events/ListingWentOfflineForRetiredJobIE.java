package ie.events;

import ie.port.OfferingIE;
import java.time.Instant;
import java.util.Objects;

/** Homa retired the last job this live listing offered, so it is no longer live until the provider offers another job and goes live again. The provider is told. */
public record ListingWentOfflineForRetiredJobIE(
        String listingId,
        String providerId,
        String jobCode,
        Instant occurredAt) implements OfferingIE {

    public ListingWentOfflineForRetiredJobIE {
        Objects.requireNonNull(listingId, "listingId");
        Objects.requireNonNull(providerId, "providerId");
        Objects.requireNonNull(jobCode, "jobCode");
        Objects.requireNonNull(occurredAt, "occurredAt");
    }
}
