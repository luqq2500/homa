package ie.events;

import ie.port.OfferingIE;
import java.time.Instant;
import java.util.Objects;

/** A live listing stopped being live, for example because no helper, or no supervisor its supervisor rule needs, is left; search stops showing it. */
public record ListingWentOfflineIE(
        String listingId,
        String providerId,
        Instant occurredAt) implements OfferingIE {

    public ListingWentOfflineIE {
        Objects.requireNonNull(listingId, "listingId");
        Objects.requireNonNull(providerId, "providerId");
        Objects.requireNonNull(occurredAt, "occurredAt");
    }
}
