package com.homa.reservation.domain.exception;

import java.time.Duration;

/** RES-5: a job lasts at least 1 hour and grows in 15-minute steps. */
public final class InvalidJobDurationException extends ReservationDomainException {

    private static final long serialVersionUID = 1L;

    public InvalidJobDurationException(Duration length) {
        super("A job lasts at least 1 hour in 15-minute steps, not " + length);
    }
}
