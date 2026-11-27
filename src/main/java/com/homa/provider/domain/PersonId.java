package com.homa.provider.domain;

import java.util.Objects;

/**
 * A real person as Verification knows them. One person keeps one personId across roles, so an owner (PRV-1) and a
 * member (HLP-1 to HLP-4) are recognised by it without the identity document number, which never leaves Verification
 * (IDN-3). Integration events carry its {@link #value()}.
 */
public record PersonId(String value) {

    public PersonId {
        Objects.requireNonNull(value, "value");
        if (value.isBlank()) {
            throw new IllegalArgumentException("A person id can't be blank");
        }
    }
}
