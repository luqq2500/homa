package com.homa.reservation.usecase.dto;

/**
 * The provider cancels a Reserved reservation (PCX-1, PCX-2, LMC-3).
 *
 * @param personId who signed in: the provider's owner, or a member rostered on the reservation (PRV-11)
 */
public record ProviderCancelReservationRequest(String reservationId, String personId) {
}
