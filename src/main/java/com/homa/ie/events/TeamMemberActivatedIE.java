package com.homa.ie.events;

import com.homa.ie.port.ProviderIE;
import java.time.Instant;

/**
 * A verified helper or supervisor joined a provider's team and is active (HLP-1 to HLP-4). Reservation keeps it as
 * a RosterableMember (ROS-3, ROS-4); Listing counts active helpers for the default maximum (OFR-5).
 *
 * @param role         HELPER or SUPERVISOR
 * @param eligibility  CITIZEN or FOREIGN_WORKER
 * @param permitExpiry the work permit's expiry as an ISO date (yyyy-MM-dd) for a foreign worker; null for a citizen
 */
public record TeamMemberActivatedIE(
        String eventId,
        String memberId,
        String providerId,
        String displayName,
        String role,
        String eligibility,
        String permitExpiry,
        Instant occurredAt) implements ProviderIE {
}
