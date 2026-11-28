package com.homa.reservation.usecase.dto;

/** The customer cancels a Reserved reservation (CCX, LMC-1, LMC-2). */
public record CustomerCancelReservationRequest(String reservationId, String customerId) {
}
