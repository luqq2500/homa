package com.homa.reservation.domain.exception;

import java.time.Instant;

/** JOB-3: a job can be marked Completed only after the job end time. */
public final class JobNotEndedException extends ReservationDomainException {

    private static final long serialVersionUID = 1L;

    public JobNotEndedException(Instant jobEnd) {
        super("The job can be marked Completed from " + jobEnd);
    }
}
