package com.homa.provider.usecase.exception;

/** The acting person may not do this to the provider: only its owner runs its account (PRV-1). */
public final class NotAllowedException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public NotAllowedException(String actingPersonId, String providerId) {
        super("Person " + actingPersonId + " doesn't run provider " + providerId + ", so may not act on it");
    }
}
