package com.homa.offering.domain.exception;

/** Only a live listing can be reserved. */
public class ListingNotLiveException extends OfferingRuleViolationException {

    public ListingNotLiveException() {
        super("The listing is not live");
    }
}
