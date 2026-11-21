package com.homa.reservation.domain;

import com.homa.reservation.domain.exception.IncompleteBookingException;
import java.time.Instant;
import java.util.Objects;

/**
 * What a customer selects to reserve a provider (RES-2): at least one of the provider's jobs, the job time (date,
 * start and hours), the number of helpers and their home address.
 */
public record Booking(
        String customerId, String providerId, JobSelection jobs, JobTime jobTime, int helpers, ServiceAddress address) {

    public Booking {
        Objects.requireNonNull(customerId, "customerId");
        Objects.requireNonNull(providerId, "providerId");
        Objects.requireNonNull(jobs, "jobs");
        Objects.requireNonNull(jobTime, "jobTime");
        Objects.requireNonNull(address, "address");
        if (helpers < 1) {
            throw new IncompleteBookingException("at least one helper");
        }
    }

    /** The same booking at another job start time, keeping its hours. */
    public Booking startingAt(Instant start) {
        return new Booking(customerId, providerId, jobs, new JobTime(start, jobTime.duration()), helpers, address);
    }

    /** The same booking at the customer's home address as it is now (RES-2, RES-8). */
    public Booking at(ServiceAddress homeAddress) {
        return new Booking(customerId, providerId, jobs, jobTime, helpers, homeAddress);
    }
}
