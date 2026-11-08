package discovery.domain;

import discovery.domain.exception.DurationOutOfRangeException;
import discovery.domain.exception.HelpersOutOfRangeException;
import discovery.domain.exception.NoJobsChosenException;
import discovery.domain.exception.StartBeyondHorizonException;
import discovery.domain.exception.StartNotOnQuarterHourException;
import discovery.domain.exception.StartTooSoonException;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.Set;

/** What the customer searches for: the jobs, a start, a duration, a number of helpers, in the city of their address. */
public record SearchCriteria(Set<JobCode> jobs, Instant start, int hours, int helpers, City city) {

    private static final long QUARTER_HOUR_SECONDS = Duration.ofMinutes(15).toSeconds();

    public SearchCriteria {
        jobs = Set.copyOf(jobs);
        Objects.requireNonNull(start);
        Objects.requireNonNull(city);
    }

    /** A search checked against Homa's ranges, minimum notice and booking horizon at {@code now}. */
    public static SearchCriteria of(
            Set<JobCode> jobs, Instant start, int hours, int helpers, City city, Instant now, SearchLimits limits) {
        if (jobs.isEmpty()) {
            throw new NoJobsChosenException();
        }
        requireWithinHomasRanges(hours, helpers, limits);
        requireBookableStart(start, now, limits);
        return new SearchCriteria(jobs, start, hours, helpers, city);
    }

    /** The time the booking would take. */
    public Instant end() {
        return start.plus(Duration.ofHours(hours));
    }

    private static void requireWithinHomasRanges(int hours, int helpers, SearchLimits limits) {
        if (hours < limits.minHours() || hours > limits.maxHours()) {
            throw new DurationOutOfRangeException(hours, limits.minHours(), limits.maxHours());
        }
        if (helpers < limits.minHelpers() || helpers > limits.maxHelpers()) {
            throw new HelpersOutOfRangeException(helpers, limits.minHelpers(), limits.maxHelpers());
        }
    }

    private static void requireBookableStart(Instant start, Instant now, SearchLimits limits) {
        if (start.getEpochSecond() % QUARTER_HOUR_SECONDS != 0 || start.getNano() != 0) {
            throw new StartNotOnQuarterHourException(start);
        }
        Instant earliest = now.plus(limits.minimumNotice());
        if (start.isBefore(earliest)) {
            throw new StartTooSoonException(start, earliest);
        }
        Instant latest = now.plus(limits.bookingHorizon());
        if (start.isAfter(latest)) {
            throw new StartBeyondHorizonException(start, latest);
        }
    }
}
