package com.homa.availability.domain;

import java.util.Objects;

/** The slot an accepted reservation holds on the provider's calendar. */
public record ReservationHold(String reservationId, TimeSlot slot) {

    public ReservationHold {
        Objects.requireNonNull(reservationId);
        Objects.requireNonNull(slot);
    }
}
