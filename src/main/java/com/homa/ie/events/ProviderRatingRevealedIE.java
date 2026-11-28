package com.homa.ie.events;

import com.homa.ie.port.RatingIE;
import java.time.Instant;

/**
 * What customers have said about a provider changed, so Listing's provider page shows these numbers (RTG-6). It is
 * published when a customer's rating of the provider is revealed (RTG-3), and again when Support removes a revealed
 * rating (RTG-11) or a Provider No Show voids the ratings of a reservation (NSH-6), because the numbers then drop. It
 * always carries the whole picture: Listing replaces what it holds.
 *
 * <p>Averages are plain decimal numbers with one decimal place, such as {@code "4.5"}, rounded half up. They cover
 * only revealed customer ratings that still stand: ratings still hidden by the double blind, voided or removed are
 * not counted.
 *
 * @param providerId             the provider the ratings are about
 * @param ratingCount            how many revealed customer ratings of the provider stand; 0 when none are left
 * @param averageStars           the average of their 1 to 5 stars; null when {@code ratingCount} is 0
 * @param qualityOfWorkAverage   the average of the Quality of work stars (RTG-7); null when no rating gave any
 * @param punctualityAverage     the average of the Punctuality stars (RTG-7); null when no rating gave any
 * @param professionalismAverage the average of the Professionalism stars (RTG-7); null when no rating gave any
 */
public record ProviderRatingRevealedIE(
        String eventId,
        String providerId,
        int ratingCount,
        String averageStars,
        String qualityOfWorkAverage,
        String punctualityAverage,
        String professionalismAverage,
        Instant occurredAt) implements RatingIE {
}
