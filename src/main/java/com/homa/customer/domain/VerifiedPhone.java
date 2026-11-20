package com.homa.customer.domain;

/**
 * A phone number that has passed the verification code check (CUS-1). The check itself is done outside the domain;
 * the domain accepts only a phone that is already verified.
 */
public record VerifiedPhone(String number) {

    public VerifiedPhone {
        Required.text(number, "a verified phone number");
    }
}
