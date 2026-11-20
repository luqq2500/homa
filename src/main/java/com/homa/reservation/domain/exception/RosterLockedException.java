package com.homa.reservation.domain.exception;

import java.time.Instant;

/** ROS-1: the roster can be set and changed only before the Check-in Period starts; then it is locked. */
public final class RosterLockedException extends ReservationDomainException {

    private static final long serialVersionUID = 1L;

    public RosterLockedException(Instant checkInStart) {
        super("The roster was locked when the Check-in Period started at " + checkInStart);
    }
}
