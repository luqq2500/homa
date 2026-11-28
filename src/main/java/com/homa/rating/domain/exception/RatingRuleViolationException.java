package com.homa.rating.domain.exception;

/** A Rating business rule was broken. Every Rating domain exception extends this one. */
public abstract class RatingRuleViolationException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    protected RatingRuleViolationException(String message) {
        super(message);
    }
}
