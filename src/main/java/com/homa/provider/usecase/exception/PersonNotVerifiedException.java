package com.homa.provider.usecase.exception;

/**
 * Provider has no identity verification for this person as an owner or member, so they can't open a provider (PRV-1)
 * or be added to a team (HLP-1 to HLP-3, IDN-1). They verify with Verification first.
 */
public final class PersonNotVerifiedException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public PersonNotVerifiedException(String personId) {
        super("Person " + personId + " hasn't passed identity verification as an owner or member (IDN-1)");
    }
}
