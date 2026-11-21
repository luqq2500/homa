package com.homa.listing.domain.exception;

/** OFR-11: the per-helper hourly rate after any discount can't be below Homa's minimum rate of RM20. */
public final class BelowMinimumRateException extends ListingRuleViolationException {

    public BelowMinimumRateException(String message) {
        super("OFR-11", message);
    }
}
