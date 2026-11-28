package com.homa.rating.domain;

import java.util.UUID;

/** Identifies one {@link Rating}. Rating owns it and generates it. */
public record RatingId(String value) {

    public RatingId {
        Ids.requireText(value, "RatingId");
    }

    /** A new, unique id. */
    public static RatingId generate() {
        return new RatingId(UUID.randomUUID().toString());
    }
}
