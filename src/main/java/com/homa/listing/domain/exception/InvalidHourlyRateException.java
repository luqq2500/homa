package com.homa.listing.domain.exception;

/** OFR-2: the per-helper hourly rate must be more than zero. */
public final class InvalidHourlyRateException extends ListingRuleViolationException {

    public InvalidHourlyRateException(String message) {
        super("OFR-2", message);
    }
}
