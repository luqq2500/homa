package reservation.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.Set;
import reservation.domain.exception.NoJobsChosenException;

/** What the customer chose: the jobs, the start, how many hours, how many helpers, and where. */
public record ReservationDetails(Set<JobCode> jobs, Instant start, int hours, int helpers, Address address) {

    public ReservationDetails {
        jobs = Set.copyOf(jobs);
        if (jobs.isEmpty()) {
            throw new NoJobsChosenException();
        }
        Objects.requireNonNull(start);
        Objects.requireNonNull(address);
    }

    /** The same details for a different start, as when booking again. */
    public ReservationDetails startingAt(Instant newStart) {
        return new ReservationDetails(jobs, newStart, hours, helpers, address);
    }

    /** When the booked time ends. */
    public Instant end() {
        return start.plusSeconds(hours * 3600L);
    }
}
