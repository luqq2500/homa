package com.homa.listing.domain.exception;

/** OFR-1: a job, tool or city that is not one of Homa's presets. */
public final class NotInPresetsException extends ListingRuleViolationException {

    public NotInPresetsException(String message) {
        super("OFR-1", message);
    }
}
