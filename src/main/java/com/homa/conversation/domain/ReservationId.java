package com.homa.conversation.domain;

/** The reservation a chat belongs to, as Reservation's events name it. */
public record ReservationId(String value) {

    public ReservationId {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("A reservation id must not be blank");
        }
    }
}
