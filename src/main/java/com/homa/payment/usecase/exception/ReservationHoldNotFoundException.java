package com.homa.payment.usecase.exception;

/**
 * No payment hold is linked to the reservation: its Requested event has not reached Payment yet, or the reservation was
 * never paid through Payment. Events about one reservation arrive in order, so this is retried and reported.
 */
public class ReservationHoldNotFoundException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public ReservationHoldNotFoundException(String reservationId) {
        super("No payment hold for reservation " + reservationId);
    }
}
