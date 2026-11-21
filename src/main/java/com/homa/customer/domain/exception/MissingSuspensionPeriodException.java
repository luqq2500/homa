package com.homa.customer.domain.exception;

/** CUS-4: Support suspends a customer for a set period, or until Support lifts it; it must choose which. */
public class MissingSuspensionPeriodException extends RuntimeException {

    public MissingSuspensionPeriodException() {
        super("CUS-4: Support must choose how long the suspension lasts");
    }
}
