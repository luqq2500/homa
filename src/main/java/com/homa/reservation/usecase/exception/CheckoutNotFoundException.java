package com.homa.reservation.usecase.exception;

/** No checkout has this id, so a payment reported against it cannot be matched to a booking. */
public final class CheckoutNotFoundException extends ReservationUsecaseException {

    private static final long serialVersionUID = 1L;

    public CheckoutNotFoundException(String checkoutId) {
        super("No checkout " + checkoutId);
    }
}
