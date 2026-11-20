package com.homa.provider.domain.exception;

/** PRV-1: a provider must be a registered business; a sole trader without registration can't join. */
public final class BusinessRegistrationRequiredException extends ProviderRuleViolationException {

    private static final long serialVersionUID = 1L;

    public BusinessRegistrationRequiredException() {
        super("A provider must submit its business registration number (PRV-1)");
    }
}
