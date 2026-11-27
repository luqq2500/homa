package com.homa.provider.usecase.exception;

/** No provider has this id. */
public final class ProviderNotFoundException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public ProviderNotFoundException(String providerId) {
        super("No provider " + providerId);
    }
}
