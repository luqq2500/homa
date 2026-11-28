package com.homa.reservation.domain.pricing;

import com.homa.kernel.Money;
import com.homa.kernel.Percentage;
import com.homa.reservation.domain.JobDuration;
import com.homa.reservation.domain.terms.ReservationTerms;

/**
 * RES-6: Total Reservation Fee = (per-helper hourly rate × hours × helpers) × (1 − discount rate, if a discount
 * applies) + travel fee. Arithmetic is exact; only the total the customer pays is rounded, once (PAY-8).
 */
public final class ReservationPricing {

    private ReservationPricing() {
    }

    public static Quote quote(ReservationTerms terms, JobDuration hours, int helpers) {
        return quote(terms, hours, helpers, terms.discount().rateFor(hours, helpers));
    }

    /**
     * The price of the same job for another number of helpers at a discount rate given, not looked up: a smaller
     * roster keeps the discount the booking had even when fewer helpers wouldn't earn it (ROS-8).
     */
    public static Quote quote(ReservationTerms terms, JobDuration hours, int helpers, Percentage discount) {
        Money discountedHour = discount.complement().of(terms.hourlyRatePerHelper().times(helpers));
        Money hoursFee = discountedHour.times(hours.inHours());
        Money total = hoursFee.plus(terms.travelFee()).roundedToSen();
        return new Quote(hoursFee, terms.travelFee(), total, discountedHour, discount);
    }
}
