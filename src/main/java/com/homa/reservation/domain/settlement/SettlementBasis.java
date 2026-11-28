package com.homa.reservation.domain.settlement;

import com.homa.kernel.Money;
import com.homa.reservation.domain.JobTime;
import com.homa.reservation.domain.ReservationValues.Version;
import com.homa.reservation.domain.pricing.Quote;
import java.time.Instant;

/**
 * What a reservation was booked with, and so all a settlement policy may read (P3, RES-8): the price its frozen
 * terms gave, its job time, and Homa's reservation values in force when it was Requested. Never the live listing and
 * never the current values.
 */
public record SettlementBasis(Quote quote, JobTime jobTime, Version values) {

    /** The amount held from the customer: the Total Reservation Fee (PAY-1). */
    public Money held() {
        return quote.total();
    }

    /** CAN-2: no fee or fine is more than this share of the Total Reservation Fee. */
    public Money cap() {
        return values.feeAndFineCap().of(quote.total());
    }

    public Money zero() {
        return Money.zero(quote.total().currency());
    }

    public Instant checkInStart() {
        return jobTime.checkInStart(values);
    }

    /** The settlement of this reservation's payment from the exact lines a policy worked out (CAN-2, PAY-8). */
    public Settlement settlement(Money exactCustomerFee, Money exactCompensation, Money exactProviderFine) {
        return Settlement.of(held(), cap(), exactCustomerFee, exactCompensation, exactProviderFine);
    }
}
