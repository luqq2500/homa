package com.homa.provider.domain.exception;

/** PRV-2: a provider must have at least one active helper to go live. Supervisors don't count (PRV-3). */
public final class NoActiveHelperException extends ProviderRuleViolationException {

    private static final long serialVersionUID = 1L;

    public NoActiveHelperException(String providerId) {
        super("Provider " + providerId + " has no active helper (PRV-2)");
    }
}
