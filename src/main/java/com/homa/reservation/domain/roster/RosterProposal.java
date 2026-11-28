package com.homa.reservation.domain.roster;

import com.homa.kernel.Money;
import com.homa.reservation.domain.pricing.Quote;
import com.homa.reservation.domain.settlement.Settlement;
import java.time.Instant;
import java.util.Objects;

/**
 * ROS-8: a provider short of helpers offers a smaller roster before the Check-in Period, and waits for the customer
 * to accept or decline. The offer shows the smaller team, the new price and what the customer would get back, all
 * worked out when it was made, because a reservation is never edited (P2). It lapses when the customer declines,
 * when the provider sets a complete roster, or when the Check-in Period starts without an answer.
 *
 * @param roster     the smaller team the customer is shown, by name and verified photo (ROS-6)
 * @param quote      the new price, with the booking discount kept
 * @param settlement the money: the new price is what the customer still pays and the refund is the difference
 * @param proposedAt when the provider made the offer
 */
public record RosterProposal(Roster roster, Quote quote, Settlement settlement, Instant proposedAt) {

    public RosterProposal {
        Objects.requireNonNull(roster, "roster");
        Objects.requireNonNull(quote, "quote");
        Objects.requireNonNull(settlement, "settlement");
        Objects.requireNonNull(proposedAt, "proposedAt");
    }

    /** The helpers the smaller roster has. */
    public int helpers() {
        return roster.helpers().size();
    }

    /** What goes back to the customer if they accept (ROS-8). */
    public Money refund() {
        return settlement.customerRefund();
    }
}
