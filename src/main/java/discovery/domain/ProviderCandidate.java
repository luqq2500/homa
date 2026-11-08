package discovery.domain;

import java.time.Duration;
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
 * @param verifiedUntil the provider's re-verification due date; a job must end by it
 */
public record ProviderCandidate(
        String providerId,
        Set<JobCode> jobs,
        int minHours,
        int maxHours,
        int minHelpers,
        int maxHelpers,
        Set<City> cities,
        Optional<Instant> nearestFreeStart,
        Instant verifiedUntil) {

    public ProviderCandidate {
        providerId = DiscoveryText.require(providerId, "A provider id");
        jobs = Set.copyOf(jobs);
        cities = Set.copyOf(cities);
        Objects.requireNonNull(nearestFreeStart);
        Objects.requireNonNull(verifiedUntil);
    }

    /** Whether a job of {@code hours} at the nearest free start ends by the due date; true when there is none. */
    boolean verifiedThroughJobOf(int hours) {
        return nearestFreeStart.map(start -> !start.plus(Duration.ofHours(hours)).isAfter(verifiedUntil)).orElse(true);
    }
}
