package com.homa.ie.events;

import com.homa.ie.port.SupportIE;
import java.time.Instant;

/**
 * A report was made on a reservation, by a party or automatically when a roster member left (RPT-8). Payment holds the
 * provider's payout until the report is resolved (RPT-6); Customer counts it as activity (CUS-8).
 *
 * <p>Reports are about a reservation, so {@code reservationId} comes first: Support's events for a reservation reach
 * their consumers in order with Reservation's own, which is what keeps a payout held until a no-show is applied.
 *
 * @param reportId     the report
 * @param category     PROVIDER_NO_SHOW, CUSTOMER_NO_SHOW, ROSTER_MEMBER_LEFT, PROPERTY_DAMAGE, SAFETY_MISCONDUCT or
 *                     OTHER (RPT-2)
 * @param reporterRole CUSTOMER or PROVIDER; a Roster Member Left report Support opens from {@code RosterMemberLeftIE}
 *                     is PROVIDER
 * @param openedAt     when the report was made
 */
public record ReportOpenedIE(
        String eventId,
        String reservationId,
        String reportId,
        String customerId,
        String providerId,
        String category,
        String reporterRole,
        Instant openedAt,
        Instant occurredAt) implements SupportIE {
}
