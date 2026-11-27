package com.homa.reservation.usecase.dto;

/** A party of the reservation looks at it.
 *
 * @param viewerId the customer's or the provider's id */
public record ViewReservationRequest(String reservationId, String viewerId) {
}
