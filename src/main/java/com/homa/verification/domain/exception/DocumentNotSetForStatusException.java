package com.homa.verification.domain.exception;

/**
 * IDN-1: a person is verified only with the identity document the country policy sets for their local or foreign
 * status (§14). Without it, they can't be verified.
 */
public final class DocumentNotSetForStatusException extends VerificationRuleViolationException {

    private static final long serialVersionUID = 1L;

    public DocumentNotSetForStatusException(String country, String status, String document) {
        super("IDN-1", "in " + country + " a " + status + " person isn't verified with " + document);
    }
}
