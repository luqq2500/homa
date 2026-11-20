package com.homa.reservation.domain.pricing;

import com.homa.kernel.Money;
import com.homa.kernel.Percentage;
import java.util.Objects;

/**
 * The price of a reservation under its terms (RES-6).
 *
 * <ul>
 *   <li>{@code hoursFee}: rate × hours × helpers × (1 − discount), exact.</li>
 *   <li>{@code travelFee}: from the terms.</li>
 *   <li>{@code total}: the Total Reservation Fee the customer pays, rounded once to the sen (PAY-8).</li>
 *   <li>{@code baseCancellationFee}: rate × helpers × 1 hour × (1 − discount), exact; it is also the discounted
 *       hourly cost of all the booked helpers. Rounded only when a settlement line is produced.</li>
 *   <li>{@code discountRate}: the discount that applied, 0% when none did.</li>
 * </ul>
 */
public record Quote(Money hoursFee, Money travelFee, Money total, Money baseCancellationFee, Percentage discountRate) {

    public Quote {
        Objects.requireNonNull(hoursFee, "hoursFee");
        Objects.requireNonNull(travelFee, "travelFee");
        Objects.requireNonNull(total, "total");
        Objects.requireNonNull(baseCancellationFee, "baseCancellationFee");
        Objects.requireNonNull(discountRate, "discountRate");
    }
}
