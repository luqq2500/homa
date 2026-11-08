package com.homa.account.domain.exception;

/** An SMS code can only be confirmed before it expires. */
public class VerificationCodeExpiredException extends AccountRuleViolationException {

    public VerificationCodeExpiredException(Object expiredAt) {
        super("The verification code expired at " + expiredAt);
    }
}
