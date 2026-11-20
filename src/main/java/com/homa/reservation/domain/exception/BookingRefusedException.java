package com.homa.reservation.domain.exception;

import com.homa.reservation.domain.BookingRefusal;

/**
 * A checkout or a reservation was refused because a booking rule did not pass. When it is thrown while a paid
 * checkout becomes a reservation, the payment is refunded in full at once (RES-12).
 */
public final class BookingRefusedException extends ReservationDomainException {

    private static final long serialVersionUID = 1L;

    private final BookingRefusal refusal;

    public BookingRefusedException(BookingRefusal refusal) {
        super("Booking refused: " + refusal);
        this.refusal = refusal;
    }

    public BookingRefusal refusal() {
        return refusal;
    }
}
