package ie.events;

import ie.port.OfferingIE;
import java.time.Instant;
import java.util.Objects;

/** Homa retired a tool, and it came off this listing; the listing stays as it was. The provider is told. */
public record ToolWithdrawnFromListingIE(
        String listingId,
        String providerId,
        String toolCode,
        Instant occurredAt) implements OfferingIE {

    public ToolWithdrawnFromListingIE {
        Objects.requireNonNull(listingId, "listingId");
        Objects.requireNonNull(providerId, "providerId");
        Objects.requireNonNull(toolCode, "toolCode");
        Objects.requireNonNull(occurredAt, "occurredAt");
    }
}
