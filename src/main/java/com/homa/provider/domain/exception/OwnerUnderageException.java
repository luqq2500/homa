package com.homa.provider.domain.exception;

/** PRV-1: an owner must be aged 18 or above, as Verification's age check decides. */
public final class OwnerUnderageException extends ProviderRuleViolationException {

    private static final long serialVersionUID = 1L;

    public OwnerUnderageException(String personId) {
        super("Person " + personId + " did not pass the age check, so can't own a provider (PRV-1)");
    }
}
