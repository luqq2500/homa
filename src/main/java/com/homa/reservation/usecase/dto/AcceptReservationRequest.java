package com.homa.reservation.usecase.dto;

/**
 * The provider accepts a Requested reservation (ACC-2).
 *
 * @param personId who signed in: the provider's owner, or a member rostered on the reservation (PRV-11)
 */
public record AcceptReservationRequest(String reservationId, String personId) {
}
