package com.homa.rating.usecase.dto;

import java.util.List;
import java.util.Map;

/**
 * The customer rates the provider, or the provider rates the customer, after the job was marked Completed (RTG-1).
 *
 * @param actorId       the acting customer's or provider's own id; it must be one of the reservation's two parties
 * @param stars         the overall stars, 1 to 5 (RTG-4)
 * @param description   optional text; null or blank for none (RTG-4)
 * @param categoryStars optional stars by category name, such as PUNCTUALITY: the customer's three categories (RTG-7)
 *                      or the provider's three (RTG-8)
 * @param mediaRefs     references to uploaded media; customers only (RTG-5)
 */
public record SubmitRatingRequest(
        String actorId,
        String reservationId,
        int stars,
        String description,
        Map<String, Integer> categoryStars,
        List<String> mediaRefs) {

    public SubmitRatingRequest {
        categoryStars = Map.copyOf(categoryStars);
        mediaRefs = List.copyOf(mediaRefs);
    }
}
