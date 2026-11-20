package com.homa.listing.domain;

import java.util.Objects;

/** A city Homa offers as a preset, such as Salak Tinggi, by its code (OFR-1). */
public record City(String code) {

    public City {
        Objects.requireNonNull(code, "code");
        if (code.isBlank()) {
            throw new IllegalArgumentException("A city code can't be blank");
        }
    }
}
