package com.homa.customer.domain;

/**
 * The person a verified identity belongs to, as Verification identifies them. One person keeps one personId across
 * every role, so Customer recognises someone by it without ever seeing their identity document number (IDN-3,
 * CUS-10). Integration events carry it as its String {@link #value()}.
 */
public record PersonId(String value) {

    public PersonId {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("A person id must not be blank");
        }
    }
}
