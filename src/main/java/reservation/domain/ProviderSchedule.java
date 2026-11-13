package reservation.domain;

import java.time.Duration;
import java.util.List;
import kernel.TimeRange;

/**
 * When a provider can take a job: their open time, the buffer they keep between jobs, and the
 * booked times of their accepted reservations.
 */
public record ProviderSchedule(List<TimeRange> openTime, Duration buffer, List<TimeRange> acceptedJobs) {

    public ProviderSchedule {
        openTime = List.copyOf(openTime);
        acceptedJobs = List.copyOf(acceptedJobs);
    }

    /** A job is free when it fits inside the open time and clashes with no accepted job. */
    public boolean isFreeFor(TimeRange job) {
        return openTime.stream().anyMatch(open -> open.covers(job)) && !clashes(job);
    }

    /**
     * A new booking cannot start within the buffer after another job or end within it before one.
     * A job that ends exactly one buffer before the next does not clash with it.
     */
    public boolean clashes(TimeRange job) {
        TimeRange withBuffer = job.widenedBy(buffer);
        return acceptedJobs.stream().anyMatch(withBuffer::overlaps);
    }
}
