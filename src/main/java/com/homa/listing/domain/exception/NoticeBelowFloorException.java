package com.homa.listing.domain.exception;

/** OFR-6: the minimum notice can't go below the acceptance policy's floor (1 hour for Instant, 2 hours for Request). */
public final class NoticeBelowFloorException extends ListingRuleViolationException {

    public NoticeBelowFloorException(String message) {
        super("OFR-6", message);
    }
}
