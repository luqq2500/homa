package com.homa.listing.domain;

import java.util.Objects;

/** A job Homa offers as a preset, such as "Kitchen cleaning", by its code (OFR-1). */
public record JobType(String code) {

    public JobType {
        Objects.requireNonNull(code, "code");
        if (code.isBlank()) {
            throw new IllegalArgumentException("A job type code can't be blank");
        }
    }
}
