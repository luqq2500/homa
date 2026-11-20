package com.homa.listing.domain.exception;

/** OFR-8: a day's working hours must end after they start. */
public final class InvalidWorkingHoursException extends ListingRuleViolationException {

    public InvalidWorkingHoursException(String message) {
        super("OFR-8", message);
    }
}
