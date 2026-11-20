package com.homa.reservation.domain;

import com.homa.reservation.domain.exception.IncompleteBookingException;
import java.util.Set;

/** The jobs the customer selected: at least one, by job code (RES-2). */
public record JobSelection(Set<String> jobCodes) {

    public JobSelection {
        jobCodes = Set.copyOf(jobCodes);
        if (jobCodes.isEmpty()) {
            throw new IncompleteBookingException("at least one job");
        }
    }

    public static JobSelection of(String... jobCodes) {
        return new JobSelection(Set.of(jobCodes));
    }

    /** RES-2: every selected job must be one of the provider's jobs. */
    public boolean isOfferedBy(Set<String> offeredJobs) {
        return offeredJobs.containsAll(jobCodes);
    }
}
