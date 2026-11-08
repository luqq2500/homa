package com.homa.account.domain.exception;

/** An SMS verification code is 6 digits. */
public class InvalidVerificationCodeException extends AccountRuleViolationException {

    public InvalidVerificationCodeException() {
        super("A verification code is 6 digits");
    }
}
