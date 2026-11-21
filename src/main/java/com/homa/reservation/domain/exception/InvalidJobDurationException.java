package com.homa.reservation.domain.exception;

import java.time.Duration;

/** A job must last some time. Whether its length is bookable is decided against the provider's bounds (RES-5). */
public final class InvalidJobDurationException extends ReservationDomainException {

    private static final long serialVersionUID = 1L;

    public InvalidJobDurationException(Duration length) {
        super("A job must last some time, not " + length);
    }
}
