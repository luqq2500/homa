package com.homa.reservation.usecase.dto;

import java.time.Instant;

/** The reservation is Reserved. */
public record AcceptReservationResponse(String reservationId, String status, Instant reservedAt) {
}
