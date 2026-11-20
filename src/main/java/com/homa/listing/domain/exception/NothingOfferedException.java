package com.homa.listing.domain.exception;

/**
 * OFR-1: a listing must cover at least one job and one city, or no customer could reserve it (a customer selects
 * one of the provider's jobs, RES-2, and must live in a covered city, RES-1).
 */
public final class NothingOfferedException extends ListingRuleViolationException {

    public NothingOfferedException(String message) {
        super("OFR-1", message);
    }
}
