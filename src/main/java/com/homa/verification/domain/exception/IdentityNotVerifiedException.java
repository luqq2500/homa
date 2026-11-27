package com.homa.verification.domain.exception;

/** IDN-1: the verification service didn't pass the person's identity document, so they can't be verified. */
public final class IdentityNotVerifiedException extends VerificationRuleViolationException {

    private static final long serialVersionUID = 1L;

    public IdentityNotVerifiedException() {
        super("IDN-1", "the identity document did not pass verification, so the person can't be verified");
    }
}
