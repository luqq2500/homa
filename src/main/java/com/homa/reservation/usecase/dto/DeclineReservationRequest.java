package com.homa.reservation.usecase.dto;

/**
 * The provider declines a Requested reservation (REQ-3).
 *
 * @param personId who signed in: the provider's owner, or a member rostered on the reservation (PRV-11)
 */
public record DeclineReservationRequest(String reservationId, String personId) {
}
