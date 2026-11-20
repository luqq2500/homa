package com.homa.listing.domain.exception;

/**
 * A provider's listing change that breaks one of the Offerings and settings rules (§3.4). Carries the rule ID so
 * support and the apps can explain the refusal in the spec's words.
 */
public abstract class ListingRuleViolationException extends RuntimeException {

    private final String ruleId;

    protected ListingRuleViolationException(String ruleId, String message) {
        super(ruleId + ": " + message);
        this.ruleId = ruleId;
    }

    public String ruleId() {
        return ruleId;
    }
}
