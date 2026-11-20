package com.homa.reservation.domain.exception;

import com.homa.reservation.domain.ReservationStatus;

/** The act is not allowed while the reservation has this status. */
public final class ReservationStatusConflictException extends ReservationDomainException {

    private static final long serialVersionUID = 1L;

    public ReservationStatusConflictException(ReservationStatus status, String act) {
        super("Cannot " + act + " a " + status + " reservation");
    }
}
