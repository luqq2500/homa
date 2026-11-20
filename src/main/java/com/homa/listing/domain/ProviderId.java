package com.homa.listing.domain;

import java.util.Objects;

/** The provider a listing and its availability belong to, as the Provider context names it. */
public record ProviderId(String value) {

    public ProviderId {
        Objects.requireNonNull(value, "value");
        if (value.isBlank()) {
            throw new IllegalArgumentException("A provider id can't be blank");
        }
    }
}
