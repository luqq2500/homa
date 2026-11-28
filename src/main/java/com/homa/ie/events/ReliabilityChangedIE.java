package com.homa.ie.events;

import com.homa.ie.port.RatingIE;
import java.time.Instant;

/**
 * A party's reliability counts changed because an incident was logged (RTG-10). The counts are over the 12 months
 * before {@code occurredAt}. Customers see a provider's cancellations, no-rosters, no-shows and members who left
 * during a job; providers see a customer's fee-paying cancellations and no-shows. A free cancellation or a
 * withdrawal is never logged, so never published.
 *
 * <p>The event always carries every count, so a consumer replaces what it holds. A count for something the party
 * can't have (a customer's no-rosters or members who left) is 0.
 *
 * @param partyId       the customer or provider the counts are about
 * @param partyRole     CUSTOMER or PROVIDER
 * @param cancellations cancellations that count: a provider's, or a customer's that paid a fee
 * @param noRosters     reservations cancelled because the provider had no complete roster (NRS-1)
 * @param noShows       confirmed no-shows by that party (NSH-1, NSH-4)
 * @param membersLeft   roster members who left during a job (RPT-8)
 */
public record ReliabilityChangedIE(
        String eventId,
        String partyId,
        String partyRole,
        int cancellations,
        int noRosters,
        int noShows,
        int membersLeft,
        Instant occurredAt) implements RatingIE {
}
