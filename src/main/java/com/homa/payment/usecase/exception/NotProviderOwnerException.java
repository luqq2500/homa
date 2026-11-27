package com.homa.payment.usecase.exception;

/** Only the provider's owner submits its payout account (PRV-1, PRV-2). */
public class NotProviderOwnerException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public NotProviderOwnerException(String personId, String providerId) {
        super(personId + " isn't the owner of provider " + providerId);
    }
}
