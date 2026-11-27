package com.homa.reservation.usecase.exception;

/** An orchestration failure of a Reservation use case: something is missing, or the actor may not act. */
public abstract class ReservationUsecaseException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    protected ReservationUsecaseException(String message) {
        super(message);
    }
}
