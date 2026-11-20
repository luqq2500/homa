package com.homa.customer.domain.exception;

/** CUS-2: a customer must agree to the Terms of Service to register. */
public class TermsOfServiceNotAcceptedException extends RuntimeException {

    public TermsOfServiceNotAcceptedException() {
        super("CUS-2: a customer must agree to the Terms of Service to register");
    }
}
