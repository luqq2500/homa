package discovery.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * A live provider as search reads them: what they offer and where, and the availability answer for the
 * search's slot.
 *
 * @param nearestFreeStart the start nearest the chosen one at which the provider is free for the whole
 *     booking, looking up to {@link MatchClassifier#NEAR_THE_CHOSEN_TIME} either side; empty when there is
 *     none. Availability answers this with {@code ProviderCalendar.nearestAvailableStart}.
 */
public record ProviderCandidate(
        String providerId,
        Set<JobCode> jobs,
        int minHours,
        int maxHours,
        int minHelpers,
        int maxHelpers,
        Set<City> cities,
        Optional<Instant> nearestFreeStart) {

    public ProviderCandidate {
        providerId = DiscoveryText.require(providerId, "A provider id");
        jobs = Set.copyOf(jobs);
        cities = Set.copyOf(cities);
        Objects.requireNonNull(nearestFreeStart);
    }
}
