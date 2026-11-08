package ie.events;

import ie.port.FeedbackIE;
import java.time.Instant;
import java.util.Objects;

/** Homa hid a rating that broke the rules; it no longer counts. */
public record RatingHiddenIE(
        String ratingId,
        String reservationId,
        String providerId,
        String reason,
        Instant occurredAt) implements FeedbackIE {

    public RatingHiddenIE {
        Objects.requireNonNull(ratingId, "ratingId");
        Objects.requireNonNull(reservationId, "reservationId");
        Objects.requireNonNull(providerId, "providerId");
        Objects.requireNonNull(reason, "reason");
        Objects.requireNonNull(occurredAt, "occurredAt");
    }
}
