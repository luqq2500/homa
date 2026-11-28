package com.homa.reservation.usecase.port.out;

import com.homa.reservation.usecase.exception.ReservationUsecaseException;

/**
 * The optimistic-lock conflict of a reservation: it was saved by someone else after it was loaded, so nothing was
 * saved. Of an acceptance and a withdrawal (or a decline, an expiry or a cancellation) racing on the same version,
 * whichever is saved first wins and the other gets this (REQ-5). The use case reloads and decides again, so it then
 * sees the winner's status.
 */
public final class ReservationChangedException extends ReservationUsecaseException {

    private static final long serialVersionUID = 1L;

    public ReservationChangedException(String reservationId) {
        super("Reservation " + reservationId + " changed since it was loaded");
    }
}
