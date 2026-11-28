package com.homa.reservation.usecase.dto;

import java.time.Instant;

/** The reservation is Expired. */
public record ExpireRequestResponse(String reservationId, String status, Instant expiredAt) {
}
