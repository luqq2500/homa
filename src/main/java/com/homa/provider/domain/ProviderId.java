package com.homa.provider.domain;

import java.util.Objects;
import java.util.UUID;

/** Identifies one provider. Integration events carry its {@link #value()}. */
public record ProviderId(String value) {

    public ProviderId {
        Objects.requireNonNull(value, "value");
        if (value.isBlank()) {
            throw new IllegalArgumentException("A provider id can't be blank");
        }
    }

    public static ProviderId generate() {
        return new ProviderId(UUID.randomUUID().toString());
    }
}
