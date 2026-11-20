package com.homa.provider.domain.exception;

/** A provider goes live once. */
public final class ProviderAlreadyLiveException extends ProviderRuleViolationException {

    private static final long serialVersionUID = 1L;

    public ProviderAlreadyLiveException(String providerId) {
        super("Provider " + providerId + " is already live");
    }
}
