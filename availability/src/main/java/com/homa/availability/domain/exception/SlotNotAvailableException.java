package com.homa.availability.domain.exception;

/** A reservation can only hold a slot the provider is free for. */
public class SlotNotAvailableException extends AvailabilityRuleViolationException {

    public SlotNotAvailableException(Object slot) {
        super("The provider is not available for " + slot);
    }
}
