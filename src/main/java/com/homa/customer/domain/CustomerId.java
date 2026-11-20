package com.homa.customer.domain;

import java.util.UUID;

/** A customer's identity. Integration events carry it as its String {@link #value()}. */
public record CustomerId(String value) {

    public CustomerId {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("A customer id must not be blank");
        }
    }

    /** A new, unique customer id. */
    public static CustomerId generate() {
        return new CustomerId(UUID.randomUUID().toString());
    }
}
