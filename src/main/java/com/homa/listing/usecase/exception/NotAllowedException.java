package com.homa.listing.usecase.exception;

/** The acting person may not do this: only a provider's owner changes its listing, and only an admin the presets. */
public final class NotAllowedException extends RuntimeException {

    public NotAllowedException(String message) {
        super(message);
    }
}
