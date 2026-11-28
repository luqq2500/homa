package com.homa.ie.events;

import com.homa.ie.port.SupportIE;
import java.time.Instant;

/**
 * Support decided a customer's dispute of a Roster Member Left report (RPT-10), after reading the chat (CHT-4):
 * {@code correctedLeftAt} is when the member really left. It equals the recorded time when the dispute is rejected.
 * Reservation works out the refund again for the corrected time and, when it is larger, publishes the difference as
 * a {@code SettlementAdjustedIE} with reason MEMBER_LEFT_CORRECTED; either way it answers with
 * {@code ReportEffectAppliedIE}. A refund already paid is never taken back.
 *
 * @param memberId        the member who left, as Provider's team-member id
 * @param correctedLeftAt when the member left, as Support decided
 */
public record RosterMemberLeftDisputeResolvedIE(
        String eventId,
        String reservationId,
        String reportId,
        String memberId,
        Instant correctedLeftAt,
        Instant decidedAt,
        Instant occurredAt) implements SupportIE {
}
