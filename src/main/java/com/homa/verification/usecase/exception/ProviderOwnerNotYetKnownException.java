package com.homa.verification.usecase.exception;

/**
 * Verification doesn't know the provider's owner yet: {@code ProviderOpenedIE} hasn't reached it. Payment accepts a
 * payout account only from a provider whose owner it knows, so the owner's event is on its way; the handler fails
 * and the relay retries it (docs/decisions/0002), instead of reporting an account as not matched.
 */
public final class ProviderOwnerNotYetKnownException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public ProviderOwnerNotYetKnownException(String providerId) {
        super("The owner of provider " + providerId + " isn't known to Verification yet");
    }
}
