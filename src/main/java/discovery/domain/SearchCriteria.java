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

/**
 * What the customer searches for: the jobs, a start, a duration, a number of helpers, in the city of their
 * address. It also keeps the earliest and latest starts the customer could book when they searched, so a
 * start suggested for a close match is always bookable.
 */
public record SearchCriteria(
        Set<JobCode> jobs, Instant start, int hours, int helpers, City city, Instant earliestStart, Instant latestStart) {

    private static final long QUARTER_HOUR_SECONDS = Duration.ofMinutes(15).toSeconds();

    public SearchCriteria {
        jobs = Set.copyOf(jobs);
        Objects.requireNonNull(start);
        Objects.requireNonNull(city);
        Objects.requireNonNull(earliestStart);
        Objects.requireNonNull(latestStart);
    }

    /** A search checked against Homa's ranges, minimum notice and booking horizon at {@code now}. */
    public static SearchCriteria of(
            Set<JobCode> jobs, Instant start, int hours, int helpers, City city, Instant now, SearchLimits limits) {
        if (jobs.isEmpty()) {
            throw new NoJobsChosenException();
        }
        requireWithinHomasRanges(hours, helpers, limits);
        Instant earliest = now.plus(limits.minimumNotice());
        Instant latest = now.plus(limits.bookingHorizon());
        requireBookableStart(start, earliest, latest);
        return new SearchCriteria(jobs, start, hours, helpers, city, earliest, latest);
    }

    /** Whether the customer could book {@code candidateStart}: from the minimum notice to the booking horizon. */
    public boolean isBookable(Instant candidateStart) {
        return !candidateStart.isBefore(earliestStart) && !candidateStart.isAfter(latestStart);
    }

    /** The time the booking would take. */
    public Instant end() {
        return start.plus(Duration.ofHours(hours));
    }

    private static void requireWithinHomasRanges(int hours, int helpers, SearchLimits limits) {
        if (hours < limits.minHours() || hours > limits.maxHours()) {
            throw new DurationOutOfRangeException(hours, limits.minHours(), limits.maxHours());
        }
        if (helpers < 1) {
            throw new HelpersOutOfRangeException(helpers);
        }
    }

    private static void requireBookableStart(Instant start, Instant earliest, Instant latest) {
        if (start.getEpochSecond() % QUARTER_HOUR_SECONDS != 0 || start.getNano() != 0) {
            throw new StartNotOnQuarterHourException(start);
        }
        if (start.isBefore(earliest)) {
            throw new StartTooSoonException(start, earliest);
        }
        if (start.isAfter(latest)) {
            throw new StartBeyondHorizonException(start, latest);
        }
    }
}
