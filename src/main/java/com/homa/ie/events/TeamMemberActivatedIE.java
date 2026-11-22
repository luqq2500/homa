package com.homa.ie.events;

import com.homa.ie.port.ProviderIE;
import java.time.Instant;

/**
 * A verified helper or supervisor joined a provider's team and is active (HLP-1 to HLP-4). Reservation keeps it as
 * a RosterableMember (ROS-3, ROS-4, ROS-6); Listing counts active helpers for the default maximum (OFR-5).
 *
 * @param role         HELPER or SUPERVISOR
 * @param personId     the person, as Verification knows them; one person belongs to one provider (HLP-10)
 * @param eligibility  LOCAL or FOREIGN_WORKER (§14)
 * @param photoVerified true when the member's face photo is verified (IDN-4); only then can they be rostered (ROS-6)
 * @param rosterableUntil the last Kuala Lumpur date (ISO yyyy-MM-dd) a job may end on: the work permit's expiry minus
 *                        Provider's 90-day buffer (HLP-3, ROS-4); null for a local member. Reservation never holds the 90
 *                        days itself (§11)
 */
public record TeamMemberActivatedIE(
        String eventId,
        String memberId,
        String personId,
        String providerId,
        String displayName,
        String role,
        String eligibility,
        boolean photoVerified,
        String rosterableUntil,
        Instant occurredAt) implements ProviderIE {
}
