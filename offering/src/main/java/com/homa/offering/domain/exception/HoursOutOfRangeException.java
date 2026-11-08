package com.homa.offering.domain.exception;

/** A provider's hours sit within Homa's duration range. */
public class HoursOutOfRangeException extends OfferingRuleViolationException {

    public HoursOutOfRangeException(int min, int max, int homaMin, int homaMax) {
        super("Hours " + min + " to " + max + " must sit within Homa's range of " + homaMin + " to " + homaMax);
    }
}
