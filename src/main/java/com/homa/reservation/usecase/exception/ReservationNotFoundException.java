package com.homa.reservation.usecase.exception;

/** No reservation has this id. */
public final class ReservationNotFoundException extends ReservationUsecaseException {

    private static final long serialVersionUID = 1L;

    public ReservationNotFoundException(String reservationId) {
        super("No reservation " + reservationId);
    }
}
