package com.homa.listing.usecase.exception;

/**
 * Listing doesn't know the provider's owner yet: {@code ProviderOpenedIE} hasn't reached it. Nothing is changed. The
 * owner is on their way, so the caller can try again shortly; infra maps this to "retry later", not to a refusal.
 */
public final class ProviderOwnerNotYetKnownException extends RuntimeException {

    public ProviderOwnerNotYetKnownException(String providerId) {
        super("The owner of provider " + providerId + " isn't known to Listing yet; try again shortly");
    }
}
