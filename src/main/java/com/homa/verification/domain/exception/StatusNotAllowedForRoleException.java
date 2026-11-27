package com.homa.verification.domain.exception;

/** CUS-10, PRV-1: a customer's or owner's local or foreign status must be one the country policy allows (§14). */
public final class StatusNotAllowedForRoleException extends VerificationRuleViolationException {

    private static final long serialVersionUID = 1L;

    public StatusNotAllowedForRoleException(String ruleId, String role, String status, String country) {
        super(ruleId, "in " + country + " " + role + " can't be " + status);
    }
}
