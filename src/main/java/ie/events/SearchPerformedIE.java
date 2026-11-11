package ie.events;

import ie.port.DiscoveryIE;
import java.time.Instant;
import java.util.Objects;
import java.util.Set;

/** A customer searched. It carries what was searched for and how many providers matched, and nothing about the customer. */
public record SearchPerformedIE(
        Set<String> jobCodes,
        Instant start,
        int hours,
        int helpers,
        String city,
        int exactMatches,
        int closeMatches,
        Instant occurredAt) implements DiscoveryIE {

    public SearchPerformedIE {
        jobCodes = Set.copyOf(jobCodes);
        Objects.requireNonNull(start, "start");
        Objects.requireNonNull(city, "city");
        Objects.requireNonNull(occurredAt, "occurredAt");
    }
}
