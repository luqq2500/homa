package com.homa.provider.domain.exception;

/** PRV-1: an owner must have a status the country policy allows for owners; in Malaysia, local only (§14). */
public final class OwnerNotLocalException extends ProviderRuleViolationException {

    private static final long serialVersionUID = 1L;

    public OwnerNotLocalException(String personId) {
        super("Person " + personId + " is not verified as local, so can't own a provider (PRV-1)");
    }
}
