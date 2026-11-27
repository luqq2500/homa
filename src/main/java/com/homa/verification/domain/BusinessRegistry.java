package com.homa.verification.domain;

import java.util.Objects;

/**
 * The registry a country's businesses are registered with, against which a Business provider's business is
 * verified (PRV-7). In Malaysia: the Companies Commission of Malaysia (SSM).
 */
public record BusinessRegistry(String name, String acronym) {

    public BusinessRegistry {
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(acronym, "acronym");
    }
}
