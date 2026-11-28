package com.homa.rating.usecase.dto;

import java.util.List;

/**
 * What the run did.
 *
 * @param revealedReservationIds reservations whose closed window this run revealed (RTG-3)
 * @param changedMeanwhileIds    reservations someone else saved first, for example a rating that came in or another
 *                               run's reveal; the next run looks at them again
 */
public record RevealClosedRatingWindowsResponse(List<String> revealedReservationIds,
        List<String> changedMeanwhileIds) {

    public RevealClosedRatingWindowsResponse {
        revealedReservationIds = List.copyOf(revealedReservationIds);
        changedMeanwhileIds = List.copyOf(changedMeanwhileIds);
    }
}
