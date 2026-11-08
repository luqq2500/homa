package com.homa.account.domain.exception;

/** An account can only be registered with a confirmed phone number. */
public class PhoneNotVerifiedException extends AccountRuleViolationException {

    public PhoneNotVerifiedException(String phone) {
        super("The phone number " + phone + " has not been confirmed");
    }
}
