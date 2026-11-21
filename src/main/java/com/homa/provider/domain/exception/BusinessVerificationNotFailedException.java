package com.homa.provider.domain.exception;

/** PRV-1: only a business whose verification failed resubmits it; others are awaiting it or already verified. */
public final class BusinessVerificationNotFailedException extends ProviderRuleViolationException {

    private static final long serialVersionUID = 1L;

    public BusinessVerificationNotFailedException(String providerId) {
        super("Provider " + providerId + " has no failed business verification to resubmit (PRV-1)");
    }
}
