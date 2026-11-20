package com.homa.listing.domain.exception;

/**
 * OFR-5: minimum and maximum hours (at least 1 hour and in 15-minute steps, RES-5) and minimum and maximum helpers
 * (at least 1) must be in order.
 */
public final class InvalidBookingBoundsException extends ListingRuleViolationException {

    public InvalidBookingBoundsException(String message) {
        super("OFR-5", message);
    }
}
