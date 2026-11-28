package com.homa.conversation.domain;

/** A customer, as Customer's events name them. */
public record CustomerId(String value) {

    public CustomerId {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("A customer id must not be blank");
        }
    }
}
