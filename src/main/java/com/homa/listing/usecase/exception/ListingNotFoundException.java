package com.homa.listing.usecase.exception;

/** The provider has no listing yet. */
public final class ListingNotFoundException extends RuntimeException {

    public ListingNotFoundException(String providerId) {
        super("No listing for provider " + providerId);
    }
}
