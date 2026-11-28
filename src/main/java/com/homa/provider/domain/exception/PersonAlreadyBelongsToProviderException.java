package com.homa.provider.domain.exception;

/**
 * HLP-10: a person belongs to only one provider at a time, as its owner or a member, active or deactivated. The
 * message doesn't name the provider they belong to: that is not for another provider to learn.
 */
public final class PersonAlreadyBelongsToProviderException extends ProviderRuleViolationException {

    private static final long serialVersionUID = 1L;

    public PersonAlreadyBelongsToProviderException(String personId) {
        super("Person " + personId + " already belongs to a provider, as its owner or a member (HLP-10)");
    }
}
