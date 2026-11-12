package ie.events;

import ie.port.OfferingIE;
import java.time.Instant;
import java.util.Objects;

/** Homa narrowed its duration range, so this live listing's hours shrank to fit inside it. The provider is told. */
public record ListingHoursNarrowedIE(
        String listingId,
        String providerId,
        int minHours,
        int maxHours,
        Instant occurredAt) implements OfferingIE {

    public ListingHoursNarrowedIE {
        Objects.requireNonNull(listingId, "listingId");
        Objects.requireNonNull(providerId, "providerId");
        Objects.requireNonNull(occurredAt, "occurredAt");
    }
}
