package com.homa.rating.usecase.dto;

import java.math.BigDecimal;
import java.util.Map;

/**
 * What is shown about a party: the revealed ratings about them and the reliability counts over the last 12 months.
 *
 * @param ratingCount      how many revealed ratings stand (RTG-6)
 * @param averageStars     their average to one decimal place; null when there are none
 * @param categoryAverages the average on each category at least one rating gave stars on, by category name (RTG-7,
 *                         RTG-8)
 * @param cancellations    a provider's cancellations, or a customer's fee-paying ones (RTG-10)
 * @param noRosters        a provider's no-rosters; 0 for a customer
 * @param noShows          the party's confirmed no-shows
 * @param membersLeft      roster members who left a provider's jobs; 0 for a customer
 */
public record ViewRatingsAndReliabilityResponse(
        String subjectId,
        String subjectRole,
        int ratingCount,
        BigDecimal averageStars,
        Map<String, BigDecimal> categoryAverages,
        int cancellations,
        int noRosters,
        int noShows,
        int membersLeft) {

    public ViewRatingsAndReliabilityResponse {
        categoryAverages = Map.copyOf(categoryAverages);
    }
}
