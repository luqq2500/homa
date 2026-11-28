package com.homa.reservation.domain.exception;

/** ROS-8: a smaller roster has at least one helper and fewer helpers than the reservation has now. */
public final class NotASmallerRosterException extends ReservationDomainException {

    private static final long serialVersionUID = 1L;

    public NotASmallerRosterException(int offeredHelpers, int helpers) {
        super("A smaller roster has at least 1 helper and fewer than the " + helpers + " the reservation has, not "
                + offeredHelpers);
    }
}
