package com.homa.reservation.usecase.exception;

/** Reservation has not heard of this customer's registration. */
public final class CustomerNotFoundException extends ReservationUsecaseException {

    private static final long serialVersionUID = 1L;

    public CustomerNotFoundException(String customerId) {
        super("No registered customer " + customerId);
    }
}
