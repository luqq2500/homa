package com.homa.reservation.domain.exception;

/** RES-2: a booking needs at least one job, a job start time, hours, at least one helper and a home address. */
public final class IncompleteBookingException extends ReservationDomainException {

    private static final long serialVersionUID = 1L;

    public IncompleteBookingException(String missing) {
        super("A booking needs " + missing);
    }
}
