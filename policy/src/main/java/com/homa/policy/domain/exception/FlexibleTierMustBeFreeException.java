package com.homa.policy.domain.exception;

/** The Flexible tier is free cancellation. */
public class FlexibleTierMustBeFreeException extends PolicyRuleViolationException {

    public FlexibleTierMustBeFreeException(Object fee) {
        super("The Flexible tier is free cancellation, so its fee must be 0%, was " + fee);
    }
}
