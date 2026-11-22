package com.homa.ie.events;

import com.homa.ie.port.ProviderIE;
import java.time.Instant;

/**
 * A live provider became bookable or stopped being bookable (PRV-2). Listing hides it from search, Reservation
 * stops admitting requests, and Notification tells the provider.
 *
 * @param reason when not bookable: NO_ACTIVE_HELPERS, NO_VERIFIED_PAYOUT_ACCOUNT, SUSPENDED or CLOSING; null when
 *               bookable again
 */
public record ProviderBookabilityChangedIE(
        String eventId, String providerId, boolean bookable, String reason, Instant occurredAt) implements ProviderIE {
}
