package com.homa.reservation.usecase.exception;

/** Reservation has not heard of this team member being activated, so they cannot be rostered. */
public final class TeamMemberNotFoundException extends ReservationUsecaseException {

    private static final long serialVersionUID = 1L;

    public TeamMemberNotFoundException(String memberId) {
        super("No team member " + memberId);
    }
}
