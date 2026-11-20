package com.homa.listing.domain.exception;

/** OFR-3: a travel fee can't be negative, and a by-city travel fee must price at least one city. */
public final class InvalidTravelFeeException extends ListingRuleViolationException {

    public InvalidTravelFeeException(String message) {
        super("OFR-3", message);
    }
}
