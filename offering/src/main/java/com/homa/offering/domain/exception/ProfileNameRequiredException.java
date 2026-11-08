package com.homa.offering.domain.exception;

/** A provider's profile needs a display name. */
public class ProfileNameRequiredException extends OfferingRuleViolationException {

    public ProfileNameRequiredException() {
        super("A profile needs a display name");
    }
}
