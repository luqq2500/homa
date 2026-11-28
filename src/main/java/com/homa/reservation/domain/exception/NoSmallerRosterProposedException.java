package com.homa.reservation.domain.exception;

/** ROS-8: the customer can answer only a smaller roster the provider has offered and that is still open. */
public final class NoSmallerRosterProposedException extends ReservationDomainException {

    private static final long serialVersionUID = 1L;

    public NoSmallerRosterProposedException() {
        super("No smaller roster is offered, so there is nothing to answer");
    }
}
