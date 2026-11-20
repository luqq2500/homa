package com.homa.payment.domain;

/** The reservation a {@link Payout} is for, as Payment knows it. Reservation issues it. */
public record ReservationId(String value) {

    public ReservationId {
        Ids.requireText(value, "ReservationId");
    }
}
