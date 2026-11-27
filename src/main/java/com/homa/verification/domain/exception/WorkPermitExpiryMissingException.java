package com.homa.verification.domain.exception;

/**
 * HLP-3, §14: a foreign person is verified only with a work permit expiry, which the country policy reads from
 * their identity document (in Malaysia, the i-Kad's date).
 */
public final class WorkPermitExpiryMissingException extends VerificationRuleViolationException {

    private static final long serialVersionUID = 1L;

    public WorkPermitExpiryMissingException(String document) {
        super("HLP-3", "a foreign person needs a work permit expiry, and their " + document + " doesn't give one");
    }
}
