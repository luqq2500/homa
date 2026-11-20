package com.homa.listing.domain.exception;

/** OFR-4: a discount needs a rate above zero, a minimum number of hours and a minimum number of helpers. */
public final class InvalidDiscountException extends ListingRuleViolationException {

    public InvalidDiscountException(String message) {
        super("OFR-4", message);
    }
}
