package com.homa.account.domain;

import com.homa.account.domain.exception.InvalidPhoneNumberException;
import java.util.regex.Pattern;

/** A Malaysian mobile number in international form, such as {@code +60123456789}. Homa sends SMS codes to it. */
public record PhoneNumber(String value) {

    private static final Pattern MALAYSIAN_MOBILE = Pattern.compile("\\+601\\d{8,9}");

    public PhoneNumber {
        if (value == null || !MALAYSIAN_MOBILE.matcher(value).matches()) {
            throw new InvalidPhoneNumberException(value);
        }
    }

    public static PhoneNumber of(String value) {
        return new PhoneNumber(value);
    }

    @Override
    public String toString() {
        return value;
    }
}
