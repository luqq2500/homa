package com.homa.provider.domain.exception;

/** HLP-4: the provider must confirm that a foreign helper or supervisor lawfully works under its registered business. */
public final class EmploymentNotConfirmedException extends ProviderRuleViolationException {

    private static final long serialVersionUID = 1L;

    public EmploymentNotConfirmedException(String memberId) {
        super("The provider has not confirmed that team member " + memberId + " is lawfully employed by it (HLP-4)");
    }
}
