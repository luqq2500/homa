package com.homa.ie.events;

import com.homa.ie.port.ProviderIE;
import java.time.Instant;

/**
 * A verified person opened a provider account as its owner (PRV-1). The provider isn't live yet: it builds its
 * team, sets up its listing and submits its payout account first (PRV-2). Listing, Payment and Verification keep
 * the owner from it, so only the owner changes the listing and submits the payout account, and the payout account's
 * holder is checked against the owner's verified identity (IDN-3). One person keeps one personId across roles.
 *
 * @param ownerPersonId the owner, as Verification knows them
 */
public record ProviderOpenedIE(String eventId, String providerId, String ownerPersonId, Instant occurredAt)
        implements ProviderIE {
}
