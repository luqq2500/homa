package com.homa.reservation.domain.exception;

import java.time.Instant;

/** ROS-1: the roster locks, or is found incomplete, only once the Check-in Period has started. */
public final class CheckInPeriodNotStartedException extends ReservationDomainException {

    private static final long serialVersionUID = 1L;

    public CheckInPeriodNotStartedException(Instant checkInStart) {
        super("The Check-in Period starts at " + checkInStart + ", and until then the roster can still change");
    }
}
