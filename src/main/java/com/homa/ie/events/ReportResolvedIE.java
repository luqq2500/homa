package com.homa.ie.events;

import com.homa.ie.port.SupportIE;
import java.time.Instant;

/**
 * A report is resolved, and its effect, if it had one, has been applied. Payment frees the payout the report held
 * (RPT-6); Customer drops it from activity (CUS-8).
 *
 * <p>A report whose decision another context must apply is not resolved by Support's decision: a confirmed no-show,
 * a refund Support decided and a corrected leave time each reach Reservation first, and Support publishes this event
 * only when Reservation answers with {@code ReportEffectAppliedIE}. A payout is therefore never freed before a
 * no-show voids it or before a decided refund lowers it. See ADR 0005.
 *
 * @param outcome CONFIRMED (a no-show was confirmed and applied), REJECTED (a no-show was not confirmed), or
 *                RESOLVED (anything else, including a Roster Member Left report that resolved itself when the
 *                Protection Window ended, RPT-10)
 */
public record ReportResolvedIE(
        String eventId,
        String reservationId,
        String reportId,
        String customerId,
        String providerId,
        String category,
        String outcome,
        Instant resolvedAt,
        Instant occurredAt) implements SupportIE {
}
