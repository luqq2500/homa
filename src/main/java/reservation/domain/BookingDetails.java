package reservation.domain;

import java.time.Duration;
import java.time.Instant;
import java.util.Set;
import kernel.TimeRange;
import reservation.domain.exception.InvalidDurationException;
import reservation.domain.exception.InvalidHelpersException;
import reservation.domain.exception.InvalidStartTimeException;
import reservation.domain.exception.JobsNotOfferedException;

/** What the customer books: the jobs, the start, the duration in whole hours and the number of helpers. */
public record BookingDetails(Set<String> jobIds, Instant start, int hours, int helpers) {

    public static final Duration START_STEP = Duration.ofMinutes(15);

    public BookingDetails {
        jobIds = Set.copyOf(jobIds);
    }

    public Instant end() {
        return start.plus(Duration.ofHours(hours));
    }

    public TimeRange bookedTime() {
        return TimeRange.of(start, end());
    }

    /** Checks the booking against the provider's offering and Homa's time rules, as of now. */
    public void checkBookable(ProviderTerms terms, HomaValues values, Instant now) {
        checkJobs(terms);
        checkStart(values, now);
        checkDuration(terms, values);
        if (helpers < terms.minHelpers() || helpers > terms.maxHelpers()) {
            throw new InvalidHelpersException(terms.minHelpers(), terms.maxHelpers());
        }
    }

    private void checkJobs(ProviderTerms terms) {
        if (jobIds.isEmpty() || !terms.jobIds().containsAll(jobIds)) {
            throw new JobsNotOfferedException();
        }
    }

    /** Start times are in 15-minute steps, at least the minimum notice from now and within the booking horizon. */
    private void checkStart(HomaValues values, Instant now) {
        if (start.toEpochMilli() % START_STEP.toMillis() != 0) {
            throw new InvalidStartTimeException("Start times are in 15-minute steps");
        }
        if (start.isBefore(now.plus(values.minimumNotice()))) {
            throw new InvalidStartTimeException("The start is at least the minimum notice from now");
        }
        if (start.isAfter(now.plus(values.bookingHorizon()))) {
            throw new InvalidStartTimeException("The start is no later than the booking horizon");
        }
    }

    /** The duration fits both Homa's range and the provider's own hours. */
    private void checkDuration(ProviderTerms terms, HomaValues values) {
        int min = Math.max(values.minimumHours(), terms.minHours());
        int max = Math.min(values.maximumHours(), terms.maxHours());
        if (hours < min || hours > max) {
            throw new InvalidDurationException(min, max);
        }
    }
}
