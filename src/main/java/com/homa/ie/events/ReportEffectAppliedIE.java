package com.homa.ie.events;

import com.homa.ie.port.ReservationIE;
import java.time.Instant;

/**
 * Reservation applied what Support decided on a report, or found nothing to apply: the answer to
 * {@code ProviderNoShowConfirmedIE}, {@code CustomerNoShowConfirmedIE}, {@code ReportRefundDecidedIE} and
 * {@code RosterMemberLeftDisputeResolvedIE}. Support resolves the report on receiving it and publishes
 * {@code ReportResolvedIE}, which is what frees the payout the report held. It is published after the events that
 * carry the effect ({@code ReservationCancelledIE}, {@code SettlementAdjustedIE}) and about the same reservation, so
 * consumers see it after them. See ADR 0005.
 *
 * @param reportId  the report whose decision was applied
 * @param effect    PROVIDER_NO_SHOW, CUSTOMER_NO_SHOW, REFUND or MEMBER_LEFT_CORRECTION
 * @param applied   false when there was nothing to apply, for example a corrected time that is no later than the
 *                  recorded one, or a decision that arrived when the reservation was already cancelled
 */
public record ReportEffectAppliedIE(
        String eventId,
        String reservationId,
        String reportId,
        String effect,
        boolean applied,
        Instant appliedAt,
        Instant occurredAt) implements ReservationIE {
}
