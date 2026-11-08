package com.homa.availability.domain.exception;

/** A business rule of a provider's availability was broken. Every availability domain exception is one of these. */
public abstract class AvailabilityRuleViolationException extends RuntimeException {

    protected AvailabilityRuleViolationException(String message) {
        super(message);
    }
}
