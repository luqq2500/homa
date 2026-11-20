package com.homa.listing.domain.exception;

/** OFR-3: a by-city travel fee must price every city the listing covers. */
public final class UnpricedCityException extends ListingRuleViolationException {

    public UnpricedCityException(String message) {
        super("OFR-3", message);
    }
}
