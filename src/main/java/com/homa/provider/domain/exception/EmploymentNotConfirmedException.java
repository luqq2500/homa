package com.homa.provider.domain.exception;

/** HLP-4: the provider must confirm that each foreign member works lawfully for its business. */
public final class EmploymentNotConfirmedException extends ProviderRuleViolationException {

    private static final long serialVersionUID = 1L;

    public EmploymentNotConfirmedException(String memberId) {
        super("The provider has not confirmed that team member " + memberId + " works lawfully for it (HLP-4)");
    }
}
