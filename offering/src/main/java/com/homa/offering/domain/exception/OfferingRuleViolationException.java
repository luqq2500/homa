package com.homa.offering.domain.exception;

/** A business rule of a provider's offering was broken. Every offering domain exception is one of these. */
public abstract class OfferingRuleViolationException extends RuntimeException {

    protected OfferingRuleViolationException(String message) {
        super(message);
    }
}
