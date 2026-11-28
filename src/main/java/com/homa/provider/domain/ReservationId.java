package com.homa.provider.domain;

import java.util.Objects;

/** Identifies one reservation, as Reservation's events carry it. Provider only keeps it to tell windows apart. */
public record ReservationId(String value) {

    public ReservationId {
        Objects.requireNonNull(value, "value");
        if (value.isBlank()) {
            throw new IllegalArgumentException("A reservation id can't be blank");
        }
    }
}
