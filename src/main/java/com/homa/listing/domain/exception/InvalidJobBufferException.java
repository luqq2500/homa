package com.homa.listing.domain.exception;

/** OFR-10: the buffer between jobs can't be negative. */
public final class InvalidJobBufferException extends ListingRuleViolationException {

    public InvalidJobBufferException(String message) {
        super("OFR-10", message);
    }
}
