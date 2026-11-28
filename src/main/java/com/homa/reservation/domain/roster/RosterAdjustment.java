package com.homa.reservation.domain.roster;

import com.homa.kernel.Money;
import com.homa.reservation.domain.pricing.Quote;
import com.homa.reservation.domain.settlement.Settlement;
import java.time.Instant;
import java.util.Objects;

/**
 * ROS-8: the customer accepted a smaller roster. The reservation goes on with fewer helpers at the new price, which
 * keeps the booking discount; Payment refunds the difference at once and lowers the provider's payout by it. It
 * replaces the price the reservation was booked at in everything that follows: a later cancellation settles against
 * it, and a reservation can be made smaller again.
 *
 * @param helpers    the helpers the reservation has now
 * @param quote      the new price
 * @param settlement the money: the new price is what the customer still pays and the refund is the difference
 * @param adjustedAt when the customer accepted
 */
public record RosterAdjustment(int helpers, Quote quote, Settlement settlement, Instant adjustedAt) {

    public RosterAdjustment {
        Objects.requireNonNull(quote, "quote");
        Objects.requireNonNull(settlement, "settlement");
        Objects.requireNonNull(adjustedAt, "adjustedAt");
    }

    /** What goes back to the customer from the held payment (ROS-8). */
    public Money refund() {
        return settlement.customerRefund();
    }
}
