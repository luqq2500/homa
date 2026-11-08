package offering.domain;

import java.util.Objects;
import java.util.Set;

/**
 * What a reservation copies from a live listing at request time. The request keeps it, so a later change
 * to the listing affects new requests only.
 *
 * @param takesRequests false while the listing is paused because the provider's re-verification lapsed
 */
public record OfferingSnapshot(
        String providerId,
        HourlyRate hourlyRate,
        TravelFee travelFee,
        HoursRange hours,
        HelpersRange helpers,
        Set<JobCode> jobs,
        Set<ToolCode> tools,
        Set<City> cities,
        CancellationTier cancellationTier,
        TeamRoster roster,
        boolean takesRequests) {

    public OfferingSnapshot {
        Objects.requireNonNull(providerId);
        Objects.requireNonNull(hourlyRate);
        Objects.requireNonNull(travelFee);
        Objects.requireNonNull(hours);
        Objects.requireNonNull(helpers);
        jobs = Set.copyOf(jobs);
        tools = Set.copyOf(tools);
        cities = Set.copyOf(cities);
        Objects.requireNonNull(cancellationTier);
        Objects.requireNonNull(roster);
    }
}
