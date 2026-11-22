package com.homa.customer.domain.exception;

/** CUS-4: Support suspends a customer for 7 days, 30 days, or until Support lifts it; its decision says which. */
public class MissingSuspensionLengthException extends RuntimeException {

    public MissingSuspensionLengthException() {
        super("CUS-4: Support's decision must say how long the suspension lasts");
    }
}
