package com.homa.availability.domain.exception;

/** Start times are in 15-minute steps. */
public class SlotStartNotOnQuarterHourException extends AvailabilityRuleViolationException {

    public SlotStartNotOnQuarterHourException(Object start) {
        super("A start time must be on a 15-minute step, was " + start);
    }
}
