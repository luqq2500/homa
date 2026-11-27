package com.homa.reservation.usecase.exception;

/** Reservation has not heard of this provider's listing, so it cannot be booked or quoted. */
public final class ProviderNotFoundException extends ReservationUsecaseException {

    private static final long serialVersionUID = 1L;

    public ProviderNotFoundException(String providerId) {
        super("No listed provider " + providerId);
    }
}
