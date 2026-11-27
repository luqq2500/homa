package com.homa.verification.domain.exception;

/** IDN-1: a person is verified only with an identity document the country policy names (§14). */
public final class DocumentNotAcceptedException extends VerificationRuleViolationException {

    private static final long serialVersionUID = 1L;

    public DocumentNotAcceptedException(String country, String document) {
        super("IDN-1", country + " doesn't verify anyone with " + document);
    }
}
