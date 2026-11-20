package com.homa.provider.domain.exception;

/** PRV-1: a provider continues onboarding (adds its team, goes live) only once its business is verified. */
public final class ProviderNotVerifiedException extends ProviderRuleViolationException {

    private static final long serialVersionUID = 1L;

    public ProviderNotVerifiedException(String providerId) {
        super("Provider " + providerId + " is not a verified business (PRV-1)");
    }
}
