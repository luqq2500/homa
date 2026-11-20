package com.homa.reservation.domain.exception;

/** A Reservation business rule was broken. Every domain exception of the Reservation context extends this. */
public abstract class ReservationDomainException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    protected ReservationDomainException(String message) {
        super(message);
    }
}
