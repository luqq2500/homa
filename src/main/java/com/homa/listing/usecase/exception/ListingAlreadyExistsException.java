package com.homa.listing.usecase.exception;

/** A provider has one listing; it changes that one instead of creating another. */
public final class ListingAlreadyExistsException extends RuntimeException {

    public ListingAlreadyExistsException(String providerId) {
        super("Provider " + providerId + " already has a listing");
    }
}
