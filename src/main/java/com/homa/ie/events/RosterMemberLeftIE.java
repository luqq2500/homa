package com.homa.ie.events;

import com.homa.ie.port.ReservationIE;
import java.time.Instant;

/**
 * The provider recorded that a roster member left during a job (RPT-8, RPT-9): the rest of the team carries on. A
 * helper's unworked time is refunded through a separate {@code SettlementAdjustedIE} with reason MEMBER_LEFT, named
 * here by {@code adjustmentId}; a supervisor leaving gives no refund and no adjustment (RPT-9). Support opens the
 * Roster Member Left report from this event (RPT-8, RPT-10); Rating counts it in the provider's reliability
 * (RPT-10, RTG-10); Notification tells the customer that a member left, <b>without the reason</b> (RPT-9).
 *
 * @param memberId     the member who left, as Provider's team-member id
 * @param memberRole   HELPER or SUPERVISOR
 * @param leftAt       when the member left
 * @param reason       ILLNESS, EMERGENCY or OTHER; never shown to the customer
 * @param adjustmentId the refund's adjustment id, null when nothing is refunded (a supervisor)
 */
public record RosterMemberLeftIE(
        String eventId,
        String reservationId,
        String customerId,
        String providerId,
        String memberId,
        String memberRole,
        Instant leftAt,
        String reason,
        String adjustmentId,
        Instant occurredAt) implements ReservationIE {
}
