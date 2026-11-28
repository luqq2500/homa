package com.homa.reservation.domain.exception;

/** ROS-8: a smaller roster is offered once at a time, so the customer answers the offer they were shown. */
public final class SmallerRosterAlreadyProposedException extends ReservationDomainException {

    private static final long serialVersionUID = 1L;

    public SmallerRosterAlreadyProposedException() {
        super("A smaller roster is already offered, and waits for the customer's answer");
    }
}
