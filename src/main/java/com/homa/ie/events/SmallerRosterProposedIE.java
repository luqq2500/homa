package com.homa.ie.events;

import com.homa.ie.port.ReservationIE;
import com.homa.kernel.Money;
import java.time.Instant;
import java.util.List;

/**
 * A provider short of helpers offered a smaller roster before the Check-in Period (ROS-8). Notification tells the
 * customer, who accepts or declines until {@code answerBy}. If they accept, the price drops with the booking discount
 * kept and {@code SettlementAdjustedIE} (reason SMALLER_ROSTER) follows; if they decline, or don't answer by the
 * Check-in Period, the roster must be complete by then or the No Roster Policy applies (ROS-7).
 *
 * @param memberIds      the smaller team on offer, as Provider's team-member ids
 * @param helpers        how many helpers the smaller roster has
 * @param newTotal       what the reservation would cost if the customer accepts: 25 × 4 × 1 × 0.9 + 20 = RM110
 * @param customerRefund what goes back to the customer if they accept: the difference, RM90
 * @param proposedAt     when the provider made the offer
 * @param answerBy       the start of the Check-in Period: the offer lapses then
 */
public record SmallerRosterProposedIE(
        String eventId,
        String reservationId,
        String customerId,
        String providerId,
        List<String> memberIds,
        int helpers,
        Money newTotal,
        Money customerRefund,
        Instant proposedAt,
        Instant answerBy,
        Instant occurredAt) implements ReservationIE {

    public SmallerRosterProposedIE {
        memberIds = List.copyOf(memberIds);
    }
}
