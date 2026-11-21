package com.homa.customer.domain.exception;

/**
 * CUS-7: a customer signs in through an identity provider, such as Google or Apple. Raised when the sign-in, its
 * issuer or its subject is missing or blank.
 */
public class MissingSignInException extends RuntimeException {

    public MissingSignInException(String missing) {
        super("CUS-7: a customer must have " + missing + " from an identity provider");
    }
}
