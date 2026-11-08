package feedback.domain;

import feedback.domain.exception.AlreadyRatedException;
import feedback.domain.exception.HelperNotOnReservationException;
import feedback.domain.exception.HidingReasonRequiredException;
import feedback.domain.exception.NotTheReservationCustomerException;
import feedback.domain.exception.RatingAlreadyHiddenException;
import feedback.domain.exception.RatingWindowClosedException;
import feedback.domain.exception.ReservationNotRateableException;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * A customer's rating of one Completed reservation: the provider rating, and optional ratings of the
 * helpers who came. A rating is an opinion: it never changes a payout, a fine or a decision. Homa can hide
 * a rating that breaks the rules, but never edits one.
 */
public final class Rating {

    private final String id;
    private final String reservationId;
    private final String customerId;
    private final String providerId;
    private final ProviderRating provider;
    private final Map<String, HelperRating> helpers;
    private final Instant submittedAt;
    private String hiddenReason;
    private Instant hiddenAt;

    private Rating(RateableReservation reservation, ProviderRating provider, Map<String, HelperRating> helpers, Instant submittedAt) {
        this.id = new RatingId().generate();
        this.reservationId = reservation.reservationId();
        this.customerId = reservation.customerId();
        this.providerId = reservation.providerId();
        this.provider = provider;
        this.helpers = helpers;
        this.submittedAt = submittedAt;
    }

    /**
     * The customer rates a Completed reservation, once, within the rating window after it became
     * Completed. Only the helpers named for the reservation can be rated.
     */
    public static Rating submit(
            RateableReservation reservation,
            String customerId,
            ProviderRating provider,
            Map<String, HelperRating> helpers,
            FeedbackLimits limits,
            Instant now) {
        requireRateableBy(reservation, customerId);
        requireInsideRatingWindow(reservation, limits, now);
        Map<String, HelperRating> rated = Map.copyOf(helpers);
        rated.keySet().stream().filter(helper -> !reservation.namedHelperIds().contains(helper)).findFirst().ifPresent(helper -> {
            throw new HelperNotOnReservationException(helper);
        });
        return new Rating(reservation, Objects.requireNonNull(provider), rated, now);
    }

    /** Homa hides a rating that breaks the rules, with a recorded reason. A hidden rating no longer counts. */
    public void hide(String reason, Instant now) {
        if (isHidden()) {
            throw new RatingAlreadyHiddenException(id);
        }
        if (reason == null || reason.isBlank()) {
            throw new HidingReasonRequiredException();
        }
        hiddenReason = reason.strip();
        hiddenAt = Objects.requireNonNull(now);
    }

    public boolean isHidden() {
        return hiddenAt != null;
    }

    public String id() {
        return id;
    }

    public String reservationId() {
        return reservationId;
    }

    public String customerId() {
        return customerId;
    }

    public String providerId() {
        return providerId;
    }

    public ProviderRating provider() {
        return provider;
    }

    public Map<String, HelperRating> helpers() {
        return helpers;
    }

    public Instant submittedAt() {
        return submittedAt;
    }

    public Optional<String> hiddenReason() {
        return Optional.ofNullable(hiddenReason);
    }

    public Optional<Instant> hiddenAt() {
        return Optional.ofNullable(hiddenAt);
    }

    private static void requireRateableBy(RateableReservation reservation, String customerId) {
        if (reservation.status() != ReservationStatus.COMPLETED) {
            throw new ReservationNotRateableException(reservation.status());
        }
        if (!reservation.customerId().equals(customerId)) {
            throw new NotTheReservationCustomerException(customerId);
        }
        if (reservation.isRated()) {
            throw new AlreadyRatedException(reservation.reservationId());
        }
    }

    /** The window includes its last moment: 14 days after 14:00 is still in, 14:01 is not. */
    private static void requireInsideRatingWindow(RateableReservation reservation, FeedbackLimits limits, Instant now) {
        Instant closesAt = reservation.completedAt().orElseThrow().plus(limits.ratingWindow());
        if (now.isAfter(closesAt)) {
            throw new RatingWindowClosedException(closesAt);
        }
    }
}
