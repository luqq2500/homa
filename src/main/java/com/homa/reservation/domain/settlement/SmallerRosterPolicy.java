package com.homa.reservation.domain.settlement;

import com.homa.kernel.Money;
import com.homa.reservation.domain.pricing.Quote;
import com.homa.reservation.domain.pricing.ReservationPricing;
import com.homa.reservation.domain.terms.ReservationTerms;
import java.time.Instant;

/**
 * ROS-8: the customer accepts a smaller roster, so the price drops with the booking discount kept, the difference is
 * refunded, and there is no fine. The re-quote is the booked job for fewer helpers at the discount the booking had:
 * 25 × 4 × 1 × 0.9 + 20 = RM110 for Aisyah, so she gets RM90 back.
 *
 * <p>In the settlement the new price is what the customer still pays for the job and the refund is the difference
 * (§12: "pays RM110, gets RM90"). Neither fine nor compensation applies. The terms are the reservation's own frozen
 * snapshot (RES-8), because the hourly rate and the travel fee are not part of what a settlement basis holds.
 */
public final class SmallerRosterPolicy implements SettlementPolicy {

    private final ReservationTerms terms;
    private final int helpers;

    public SmallerRosterPolicy(ReservationTerms terms, int helpers) {
        this.terms = terms;
        this.helpers = helpers;
    }

    /** The booked job's new price for the smaller roster, keeping the discount the booking was priced with. */
    public Quote requote(SettlementBasis basis) {
        return ReservationPricing.quote(terms, basis.jobTime().duration(), helpers, basis.quote().discountRate());
    }

    /**
     * The new price is already rounded once to the sen (PAY-8), and the refund is what was held minus it. The cap on
     * fees and fines (CAN-2) doesn't apply: the new price is the job's price, not a fee, and it is below what was held.
     */
    @Override
    public Settlement settle(SettlementBasis basis, Instant at) {
        Money newTotal = requote(basis).total();
        return new Settlement(newTotal, basis.held().minus(newTotal), basis.zero(), basis.zero(), newTotal);
    }
}
