package com.homa.rating.usecase.dto;

/**
 * Support removes a rating with abusive content or personal contact or address details (RTG-11).
 *
 * @param actingPersonId the person acting
 * @param actingRole     the acting person's role; only SUPPORT may remove a rating
 */
public record RemoveRatingRequest(String actingPersonId, String actingRole, String reservationId, String ratingId) {
}
