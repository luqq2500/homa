package com.homa.ie.events;

import com.homa.ie.port.ProviderIE;
import java.time.Instant;

/**
 * A verified helper or supervisor joined a provider's team and is active (HLP-1 to HLP-4). Reservation keeps it as
 * a RosterableMember (ROS-3, ROS-4); Listing counts active helpers for the default maximum (OFR-5).
 *
 * @param role         HELPER or SUPERVISOR
 * @param eligibility  CITIZEN or FOREIGN_WORKER
 * @param rosterableUntil the last Kuala Lumpur date (ISO yyyy-MM-dd) a job may end on: the work permit's expiry minus
 *                        Provider's 90-day buffer (HLP-3, ROS-4); null for a citizen. Reservation never holds the 90
 *                        days itself (§11)
 */
public record TeamMemberActivatedIE(
        String eventId,
        String memberId,
        String providerId,
        String displayName,
        String role,
        String eligibility,
        String rosterableUntil,
        Instant occurredAt) implements ProviderIE {
}
