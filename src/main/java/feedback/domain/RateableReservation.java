package feedback.domain;

import feedback.domain.exception.FeedbackDetailRequiredException;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * A reservation as feedback knows it: who the customer and provider are, which helpers were named, where
 * it stands, when it became Completed, and whether it has already been rated.
 */
public final class RateableReservation {

    private final String reservationId;
    private final ReservationStatus status;
    private final String customerId;
    private final String providerId;
    private final Set<String> namedHelperIds;
    private final Instant completedAt;
    private final boolean rated;

    private RateableReservation(
            String reservationId,
            ReservationStatus status,
            String customerId,
            String providerId,
            Set<String> namedHelperIds,
            Instant completedAt,
            boolean rated) {
        this.reservationId = FeedbackText.require(reservationId, "A reservation id");
        this.status = Objects.requireNonNull(status);
        this.customerId = FeedbackText.require(customerId, "A customer id");
        this.providerId = FeedbackText.require(providerId, "A provider id");
        this.namedHelperIds = Set.copyOf(namedHelperIds);
        this.completedAt = completedAt;
        this.rated = rated;
    }

    /** A reservation that became Completed at {@code completedAt}, not yet rated. */
    public static RateableReservation completed(
            String reservationId, String customerId, String providerId, Set<String> namedHelperIds, Instant completedAt) {
        return new RateableReservation(
                reservationId, ReservationStatus.COMPLETED, customerId, providerId, namedHelperIds, Objects.requireNonNull(completedAt), false);
    }

    /** A reservation that has not become Completed. */
    public static RateableReservation notCompleted(
            String reservationId, ReservationStatus status, String customerId, String providerId, Set<String> namedHelperIds) {
        if (status == ReservationStatus.COMPLETED) {
            throw new FeedbackDetailRequiredException("The time a Completed reservation became Completed");
        }
        return new RateableReservation(reservationId, status, customerId, providerId, namedHelperIds, null, false);
    }

    /** The same reservation, now rated. */
    public RateableReservation rated() {
        return new RateableReservation(reservationId, status, customerId, providerId, namedHelperIds, completedAt, true);
    }

    public String reservationId() {
        return reservationId;
    }

    public ReservationStatus status() {
        return status;
    }

    public String customerId() {
        return customerId;
    }

    public String providerId() {
        return providerId;
    }

    public Set<String> namedHelperIds() {
        return namedHelperIds;
    }

    public Optional<Instant> completedAt() {
        return Optional.ofNullable(completedAt);
    }

    public boolean isRated() {
        return rated;
    }
}
