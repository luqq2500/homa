package com.homa.provider.domain;

import java.util.Objects;
import java.util.UUID;

/** Identifies one helper or supervisor: one per person. Integration events carry its {@link #value()}. */
public record TeamMemberId(String value) {

    public TeamMemberId {
        Objects.requireNonNull(value, "value");
        if (value.isBlank()) {
            throw new IllegalArgumentException("A team member id can't be blank");
        }
    }

    public static TeamMemberId generate() {
        return new TeamMemberId(UUID.randomUUID().toString());
    }
}
