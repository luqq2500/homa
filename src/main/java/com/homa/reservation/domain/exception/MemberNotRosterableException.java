package com.homa.reservation.domain.exception;

/**
 * ROS-3, ROS-4: only the provider's own active helpers and supervisors can be rostered, and a foreign member only
 * on a job that ends by the last date Provider says they may work on.
 */
public final class MemberNotRosterableException extends ReservationDomainException {

    private static final long serialVersionUID = 1L;

    public MemberNotRosterableException(String memberId) {
        super("Team member " + memberId + " cannot be rostered on this reservation");
    }
}
