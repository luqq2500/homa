package com.homa.customer.domain.exception;

import com.homa.customer.domain.SignInId;

/** CUS-7: one identity-provider account can hold only one Homa customer account. */
public class SignInAlreadyHoldsAccountException extends RuntimeException {

    public SignInAlreadyHoldsAccountException(SignInId signInId) {
        super("CUS-7: this " + signInId.issuer() + " account already holds a Homa customer account");
    }
}
