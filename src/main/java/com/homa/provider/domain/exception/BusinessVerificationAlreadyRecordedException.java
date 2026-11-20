package com.homa.provider.domain.exception;

/** PRV-1: a business verification result is recorded once, while the provider awaits it. */
public final class BusinessVerificationAlreadyRecordedException extends ProviderRuleViolationException {

    private static final long serialVersionUID = 1L;

    public BusinessVerificationAlreadyRecordedException(String providerId) {
        super("Provider " + providerId + " is not awaiting business verification (PRV-1)");
    }
}
