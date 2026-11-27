package com.homa.verification.domain.exception;

/**
 * A Verification business rule refused an act. Each subclass guards one rule, and {@link #ruleId()} names it, so
 * the use case and Support can trace a refusal to the rules page. A message never holds an identity document
 * number (IDN-3).
 */
public abstract class VerificationRuleViolationException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final String ruleId;

    protected VerificationRuleViolationException(String ruleId, String message) {
        super(ruleId + ": " + message);
        this.ruleId = ruleId;
    }

    /** The rule that refused the act, for example "IDN-1". */
    public String ruleId() {
        return ruleId;
    }
}
