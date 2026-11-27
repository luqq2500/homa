package com.homa.verification.domain;

import com.homa.verification.domain.exception.MissingIdentityDetailException;

/** IDN-2: the verified person's name, as their identity document gives it. */
public record PersonName(String value) {

    public PersonName {
        if (value == null || value.isBlank()) {
            throw new MissingIdentityDetailException("a name");
        }
    }
}
