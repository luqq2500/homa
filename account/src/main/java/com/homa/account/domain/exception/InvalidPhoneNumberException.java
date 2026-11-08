package com.homa.account.domain.exception;

/** Homa verifies Malaysian mobile numbers, written as +601 followed by 8 or 9 digits. */
public class InvalidPhoneNumberException extends AccountRuleViolationException {

    public InvalidPhoneNumberException(String phone) {
        super("Not a Malaysian mobile number: " + phone);
    }
}
