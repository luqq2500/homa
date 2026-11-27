package com.homa.reservation.usecase.dto;

/** The provider accepts a Requested reservation (ACC-2). */
public record AcceptReservationRequest(String reservationId, String providerId) {
}
