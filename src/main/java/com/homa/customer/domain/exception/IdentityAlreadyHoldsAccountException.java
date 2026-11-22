package com.homa.customer.domain.exception;

/**
 * CUS-10: one verified identity holds one customer account. Raised when a person's identity, already held by one
 * customer account, is verified on another, for example after signing up again with a different Google account.
 */
public class IdentityAlreadyHoldsAccountException extends RuntimeException {

    public IdentityAlreadyHoldsAccountException() {
        super("CUS-10: this verified identity already holds a Homa customer account");
    }
}
