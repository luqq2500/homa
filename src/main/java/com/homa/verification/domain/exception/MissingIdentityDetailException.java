package com.homa.verification.domain.exception;

/** IDN-2: Homa stores each verified person's name and identity document number; one of them is missing. */
public final class MissingIdentityDetailException extends VerificationRuleViolationException {

    private static final long serialVersionUID = 1L;

    public MissingIdentityDetailException(String detail) {
        super("IDN-2", "a verified person must have " + detail);
    }
}
