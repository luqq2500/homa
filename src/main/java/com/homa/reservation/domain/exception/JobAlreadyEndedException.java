package com.homa.reservation.domain.exception;

import java.time.Instant;

/** A Reserved reservation can be cancelled only before its job end time (§6.2, §6.5). */
public final class JobAlreadyEndedException extends ReservationDomainException {

    private static final long serialVersionUID = 1L;

    public JobAlreadyEndedException(Instant jobEnd) {
        super("The job ended at " + jobEnd + ", so the reservation can no longer be cancelled");
    }
}
