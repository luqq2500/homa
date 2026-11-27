package com.homa.payment.usecase.exception;

/** No payment hold is open for the checkout: it was never started, or its start has not reached Payment yet. */
public class PaymentHoldNotFoundException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public PaymentHoldNotFoundException(String checkoutId) {
        super("No payment hold for checkout " + checkoutId);
    }
}
