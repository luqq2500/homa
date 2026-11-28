package com.homa.rating.domain;

import com.homa.kernel.Money;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

/**
 * A Reserved reservation that was cancelled, as far as Rating cares: why, what the customer paid the provider and
 * what the provider was fined. From these it says which side's reliability the cancellation counts against and
 * whether it voids ratings.
 *
 * @param customerFee  what the customer pays the provider; zero for a free cancellation
 * @param providerFine the fine on the provider; zero when none
 */
public record Cancellation(
        ReservationId reservationId, CancellationCause cause, Money customerFee, Money providerFine,
        Instant cancelledAt) {

    public Cancellation {
        Objects.requireNonNull(reservationId, "reservationId");
        Objects.requireNonNull(cause, "cause");
        Objects.requireNonNull(customerFee, "customerFee");
        Objects.requireNonNull(providerFine, "providerFine");
        Objects.requireNonNull(cancelledAt, "cancelledAt");
    }

    /**
     * What this cancellation adds to the reliability of a party on {@code role}'s side, if anything (RTG-10); see
     * {@link CancellationCause}. A withdrawal never reaches Rating.
     */
    public Optional<IncidentKind> countsAgainst(PartyRole role) {
        return cause.countsAgainst(role, customerFee, providerFine);
    }

    /**
     * A Provider No Show reported in the Protection Window can be confirmed even after the job was marked
     * Completed: the reservation becomes Cancelled and its ratings are removed (NSH-6).
     */
    public boolean voidsRatings() {
        return cause == CancellationCause.PROVIDER_NO_SHOW;
    }
}
