package com.homa.payment.usecase.exception;

/**
 * Payment doesn't know the provider's owner yet: {@code ProviderOpenedIE} hasn't reached it. Nothing is changed. The
 * owner is on their way, so the caller can try again shortly; infra maps this to "retry later", not to a refusal.
 */
public class ProviderOwnerNotYetKnownException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public ProviderOwnerNotYetKnownException(String providerId) {
        super("The owner of provider " + providerId + " isn't known to Payment yet; try again shortly");
    }
}
