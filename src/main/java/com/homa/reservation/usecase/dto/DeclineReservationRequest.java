package com.homa.reservation.usecase.dto;

/** The provider declines a Requested reservation (REQ-3). */
public record DeclineReservationRequest(String reservationId, String providerId) {
}
