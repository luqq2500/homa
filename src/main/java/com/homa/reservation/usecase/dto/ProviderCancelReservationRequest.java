package com.homa.reservation.usecase.dto;

/** The provider cancels a Reserved reservation (PCX-1, PCX-2, LMC-3). */
public record ProviderCancelReservationRequest(String reservationId, String providerId) {
}
