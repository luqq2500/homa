package com.homa.reservation.domain.exception;

/** ROS-2: the roster must include the booked number of helpers, each member once. */
public final class IncompleteRosterException extends ReservationDomainException {

    private static final long serialVersionUID = 1L;

    public IncompleteRosterException(String reason) {
        super("Incomplete roster: " + reason);
    }
}
