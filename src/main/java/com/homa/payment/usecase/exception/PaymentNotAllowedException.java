package com.homa.payment.usecase.exception;

/** Only the customer who started a checkout pays for it. */
public class PaymentNotAllowedException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public PaymentNotAllowedException(String customerId, String checkoutId) {
        super("Customer " + customerId + " did not start checkout " + checkoutId);
    }
}
