package com.homa.reservation.domain.terms;

import com.homa.kernel.Money;
import java.util.Objects;

/**
 * RES-8: the provider's terms frozen into a reservation: per-helper hourly rate, travel fee (already resolved for
 * the reservation's city), discount, acceptance policy and cancellation tier. Later changes by the provider apply
 * only to new reservations. Every price and policy of the reservation reads this snapshot, never the live listing.
 */
public record ReservationTerms(
        Money hourlyRatePerHelper,
        Money travelFee,
        DiscountRule discount,
        AcceptancePolicy acceptancePolicy,
        CancellationTier cancellationTier) {

    public ReservationTerms {
        Objects.requireNonNull(hourlyRatePerHelper, "hourlyRatePerHelper");
        Objects.requireNonNull(travelFee, "travelFee");
        Objects.requireNonNull(discount, "discount");
        Objects.requireNonNull(acceptancePolicy, "acceptancePolicy");
        Objects.requireNonNull(cancellationTier, "cancellationTier");
    }
}
