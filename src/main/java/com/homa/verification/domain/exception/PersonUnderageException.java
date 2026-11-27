package com.homa.verification.domain.exception;

/** CUS-10, PRV-1, HLP-1: every customer, owner and member must be aged 18 or above. */
public final class PersonUnderageException extends VerificationRuleViolationException {

    private static final long serialVersionUID = 1L;

    public PersonUnderageException(String ruleId, String role, int minimumAge) {
        super(ruleId, role + " must be aged " + minimumAge + " or above");
    }
}
