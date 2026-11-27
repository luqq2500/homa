package com.homa.payment.usecase.exception;

/** The provider never submitted a payout account to Payment. */
public class PayoutAccountNotFoundException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public PayoutAccountNotFoundException(String providerId) {
        super("No payout account for provider " + providerId);
    }
}
