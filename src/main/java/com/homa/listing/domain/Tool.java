package com.homa.listing.domain;

import java.util.Objects;

/** A tool Homa offers as a preset, such as "HEPA vacuum", by its code (OFR-1). */
public record Tool(String code) {

    public Tool {
        Objects.requireNonNull(code, "code");
        if (code.isBlank()) {
            throw new IllegalArgumentException("A tool code can't be blank");
        }
    }
}
