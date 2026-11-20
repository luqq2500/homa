package com.homa.payment.domain.exception;

/** A Payment business rule was broken. Every Payment domain exception extends this one. */
public abstract class PaymentRuleViolationException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    protected PaymentRuleViolationException(String message) {
        super(message);
    }
}
