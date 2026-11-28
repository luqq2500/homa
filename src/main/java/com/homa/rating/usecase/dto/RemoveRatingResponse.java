package com.homa.rating.usecase.dto;

/**
 * The rating is removed.
 *
 * @param authorRole CUSTOMER or PROVIDER: whose rating it was
 */
public record RemoveRatingResponse(String reservationId, String ratingId, String authorRole) {
}
