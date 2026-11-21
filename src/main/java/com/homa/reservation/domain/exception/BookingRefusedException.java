package com.homa.reservation.domain.exception;

import com.homa.reservation.domain.BookingRefusal;
import com.homa.reservation.domain.CheckoutRejection;

/**
 * A checkout or a reservation was refused because the time is taken or a booking rule did not pass. When it is
 * thrown while a confirmed payment was to make the reservation Requested, the payment is refunded in full at once
 * for {@link #rejection()} (P1, RES-11).
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

    /** TIME_TAKEN or RULE_FAILED, the reason {@code ReservationCheckoutRejectedIE} carries. */
    public CheckoutRejection rejection() {
        return refusal.rejection();
    }
}
