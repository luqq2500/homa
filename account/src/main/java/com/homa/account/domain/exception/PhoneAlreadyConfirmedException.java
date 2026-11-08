package com.homa.account.domain.exception;

/** A confirmed phone verification is not confirmed again. */
public class PhoneAlreadyConfirmedException extends AccountRuleViolationException {

    public PhoneAlreadyConfirmedException(String phone) {
        super("The phone number " + phone + " is already confirmed");
    }
}
