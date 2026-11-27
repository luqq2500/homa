package com.homa.verification.domain;

import java.util.UUID;

/**
 * Identifies one real person, whatever their roles: the same personId as a customer, an owner and a member. Other
 * contexts recognise a person by it, never by the identity document number (IDN-3). Integration events carry it as
 * its String {@link #value()}.
 */
public record PersonId(String value) {

    public PersonId {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("A person id must not be blank");
        }
    }

    /** A new, unique person id. */
    public static PersonId generate() {
        return new PersonId(UUID.randomUUID().toString());
    }
}
