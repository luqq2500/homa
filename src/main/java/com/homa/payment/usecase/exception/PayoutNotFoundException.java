package com.homa.payment.usecase.exception;

/** No payout was opened for the reservation: Payment never saw it Reserved. */
public class PayoutNotFoundException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public PayoutNotFoundException(String reservationId) {
        super("No payout for reservation " + reservationId);
    }
}
