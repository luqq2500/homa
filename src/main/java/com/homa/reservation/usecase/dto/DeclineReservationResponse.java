package com.homa.reservation.usecase.dto;

import java.time.Instant;

/** The reservation is Declined. */
public record DeclineReservationResponse(String reservationId, String status, Instant declinedAt) {
}
