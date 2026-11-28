package com.homa.ie.events;

import com.homa.ie.port.ProviderIE;
import java.time.Instant;

/**
 * A member stopped being active on a provider's team: the provider deactivated them (HLP-5) or removed them (HLP-8).
 * Reservation takes them off the people who can be rostered (HLP-6) and, when they are on an upcoming roster, tells
 * the provider to replace them before the Check-in Period starts (ROS-5); Listing counts one active helper fewer
 * (OFR-5). A deactivated member can be reactivated, which Provider announces with {@code TeamMemberActivatedIE}; a
 * removed member comes back only as a new member (HLP-8).
 *
 * @param reason DEACTIVATED or REMOVED
 */
public record TeamMemberDeactivatedIE(
        String eventId,
        String memberId,
        String personId,
        String providerId,
        String reason,
        Instant occurredAt) implements ProviderIE {
}
