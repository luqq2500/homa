package catalogue.domain;

import catalogue.domain.exception.EmptyPresetSearchException;
import catalogue.domain.exception.PresetSearchOutOfRangeException;
import java.util.Objects;
import java.util.Set;

/**
 * The search a featured search fills in: the jobs, the duration in hours and the number of helpers. The
 * customer picks the date and time, and the city comes from their address.
 *
 * <p>It needs at least one job, one hour and one helper. Homa's duration range (1 to 6 hours at launch) is a
 * Homa-set value that can change, so it is checked when the search runs, not when it is featured.
 */
public record PresetSearch(Set<JobCode> jobs, int hours, int helpers) {

    public PresetSearch {
        jobs = Set.copyOf(Objects.requireNonNull(jobs));
        if (jobs.isEmpty()) {
            throw new EmptyPresetSearchException();
        }
        if (hours < 1) {
            throw new PresetSearchOutOfRangeException("hour", hours);
        }
        if (helpers < 1) {
            throw new PresetSearchOutOfRangeException("helper", helpers);
        }
    }

    public static PresetSearch of(Set<JobCode> jobs, int hours, int helpers) {
        return new PresetSearch(jobs, hours, helpers);
    }
}
