package ie.events;

import ie.port.OfferingIE;
import java.time.Instant;
import java.util.Objects;

/** Homa retired a job, and it came off this listing; the listing still has other jobs, and is as live or paused as it was. The provider is told. */
public record JobWithdrawnFromListingIE(
        String listingId,
        String providerId,
        String jobCode,
        Instant occurredAt) implements OfferingIE {

    public JobWithdrawnFromListingIE {
        Objects.requireNonNull(listingId, "listingId");
        Objects.requireNonNull(providerId, "providerId");
        Objects.requireNonNull(jobCode, "jobCode");
        Objects.requireNonNull(occurredAt, "occurredAt");
    }
}
