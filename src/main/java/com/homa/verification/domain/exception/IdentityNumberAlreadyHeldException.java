package com.homa.verification.domain.exception;

/**
 * IDN-3: the identity document number recognises one person, so it maps to exactly one personId. A number already
 * verified for another person can't verify someone else; that is how one verified identity holds one customer
 * account (CUS-10) and one place in one provider (HLP-10). The message names neither the number nor its holder.
 */
public final class IdentityNumberAlreadyHeldException extends VerificationRuleViolationException {

    private static final long serialVersionUID = 1L;

    public IdentityNumberAlreadyHeldException() {
        super("IDN-3", "this identity document is already verified for another person");
    }
}
