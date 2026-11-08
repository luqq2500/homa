package ie.events;

import ie.port.FeedbackIE;
import java.time.Instant;
import java.util.Objects;
import java.util.Set;

/** A customer rated a Completed reservation: the provider's overall stars, and which helpers were rated. */
public record RatingSubmittedIE(
        String ratingId,
        String reservationId,
        String customerId,
        String providerId,
        int overallStars,
        boolean hasWrittenFeedback,
        Set<String> ratedHelperIds,
        Instant occurredAt) implements FeedbackIE {

    public RatingSubmittedIE {
        Objects.requireNonNull(ratingId, "ratingId");
        Objects.requireNonNull(reservationId, "reservationId");
        Objects.requireNonNull(customerId, "customerId");
        Objects.requireNonNull(providerId, "providerId");
        ratedHelperIds = Set.copyOf(ratedHelperIds);
        Objects.requireNonNull(occurredAt, "occurredAt");
    }
}
