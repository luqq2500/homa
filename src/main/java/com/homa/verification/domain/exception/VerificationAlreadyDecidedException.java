package com.homa.verification.domain.exception;

/** IDN-2: a verification result is kept as recorded. A verification already verified or refused isn't decided again. */
public final class VerificationAlreadyDecidedException extends VerificationRuleViolationException {

    private static final long serialVersionUID = 1L;

    public VerificationAlreadyDecidedException(String state) {
        super("IDN-2", "this verification is already " + state + ", so it can't be decided again");
    }
}
