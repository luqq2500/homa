package com.homa.reservation.domain.replica;

import com.homa.kernel.Money;
import com.homa.reservation.domain.terms.AcceptancePolicy;
import com.homa.reservation.domain.terms.CancellationTier;
import com.homa.reservation.domain.terms.DiscountRule;
import com.homa.reservation.domain.terms.ReservationTerms;
import java.util.Objects;

/** The terms a provider offers today (OFR-2 to OFR-4, OFR-9). A reservation freezes them when made (RES-8). */
public record OfferedTerms(
        Money hourlyRatePerHelper,
        TravelFeeRule travelFee,
        DiscountRule discount,
        AcceptancePolicy acceptancePolicy,
        CancellationTier cancellationTier) {

    public OfferedTerms {
        Objects.requireNonNull(hourlyRatePerHelper, "hourlyRatePerHelper");
        Objects.requireNonNull(travelFee, "travelFee");
        Objects.requireNonNull(discount, "discount");
        Objects.requireNonNull(acceptancePolicy, "acceptancePolicy");
        Objects.requireNonNull(cancellationTier, "cancellationTier");
    }

    /** RES-8: the terms frozen for a reservation in this city. */
    public ReservationTerms snapshotFor(String city) {
        return new ReservationTerms(
                hourlyRatePerHelper, travelFee.feeFor(city), discount, acceptancePolicy, cancellationTier);
    }
}
