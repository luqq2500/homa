package com.homa.reservation.usecase.dto;

import java.time.Instant;

/** The reservation is Completed. */
public record CompleteReservationResponse(String reservationId, String status, Instant completedAt) {
}
