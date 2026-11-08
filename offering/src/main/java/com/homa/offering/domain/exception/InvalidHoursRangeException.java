package com.homa.offering.domain.exception;

/** A provider's minimum hours are not above their maximum. */
public class InvalidHoursRangeException extends OfferingRuleViolationException {

    public InvalidHoursRangeException(int min, int max) {
        super("The minimum hours " + min + " are above the maximum " + max);
    }
}
