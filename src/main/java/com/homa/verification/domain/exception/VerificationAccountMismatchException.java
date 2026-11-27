package com.homa.verification.domain.exception;

/**
 * CUS-3: a customer verifies for the customer account that asked, at its first reservation, so a customer's
 * verification names that account. An owner or a member is recognised by personId alone, so theirs names none.
 */
public final class VerificationAccountMismatchException extends VerificationRuleViolationException {

    private static final long serialVersionUID = 1L;

    public VerificationAccountMismatchException(String message) {
        super("CUS-3", message);
    }
}
