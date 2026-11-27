package com.homa.reservation.usecase.exception;

/** The actor is not the party of the reservation that may do this: its provider, or its customer. */
public final class NotReservationPartyException extends ReservationUsecaseException {

    private static final long serialVersionUID = 1L;

    public NotReservationPartyException(String actorId, String reservationId) {
        super(actorId + " may not act on reservation " + reservationId);
    }
}
