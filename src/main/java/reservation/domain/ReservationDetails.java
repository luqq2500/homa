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

    /** The same jobs, hours and helpers, at a new start and the customer's current address, as when booking again. */
    public ReservationDetails forBookingAgain(Instant newStart, Address currentAddress) {
        return new ReservationDetails(jobs, newStart, hours, helpers, currentAddress);
    }

    /** When the booked time ends. */
    public Instant end() {
        return start.plusSeconds(hours * 3600L);
    }
}
