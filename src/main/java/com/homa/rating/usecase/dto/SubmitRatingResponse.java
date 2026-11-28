package com.homa.rating.usecase.dto;

import java.time.Instant;

/**
 * The rating was taken.
 *
 * @param authorRole CUSTOMER or PROVIDER
 * @param revealed   true when this rating completed the pair, so both are shown now (RTG-3)
 * @param revealedAt when both were revealed; null while the ratings are still hidden
 */
public record SubmitRatingResponse(
        String reservationId, String ratingId, String authorRole, boolean revealed, Instant revealedAt) {
}
