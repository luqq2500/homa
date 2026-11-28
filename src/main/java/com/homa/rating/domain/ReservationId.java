package com.homa.rating.domain;

/** The reservation a {@link RatingSession} is for, as Rating knows it. Reservation issues it. */
public record ReservationId(String value) {

    public ReservationId {
        Ids.requireText(value, "ReservationId");
    }
}
