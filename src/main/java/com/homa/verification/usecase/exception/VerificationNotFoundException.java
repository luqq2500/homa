package com.homa.verification.usecase.exception;

/** The verification service named a check Homa didn't start. */
public final class VerificationNotFoundException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public VerificationNotFoundException(String serviceReference) {
        super("No verification is known by the service reference " + serviceReference);
    }
}
