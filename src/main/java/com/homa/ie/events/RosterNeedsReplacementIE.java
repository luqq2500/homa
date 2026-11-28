package com.homa.ie.events;

import com.homa.ie.port.ReservationIE;
import java.time.Instant;

/**
 * A member on a Reserved reservation's roster stopped being active before its Check-in Period (ROS-5): the provider
 * deactivated or removed them, or Support or monitoring suspended them. Notification tells the provider, who must
 * set a roster without them before {@code checkInStart}. If the roster is still incomplete when the Check-in Period
 * starts, the reservation is cancelled under the No Roster Policy (ROS-7, NRS-1 to NRS-3).
 *
 * @param memberId     the rostered member who is no longer active, as Provider's team-member id
 * @param checkInStart when the roster locks: the provider replaces the member before this moment
 */
public record RosterNeedsReplacementIE(
        String eventId,
        String reservationId,
        String providerId,
        String memberId,
        Instant checkInStart,
        Instant occurredAt) implements ReservationIE {
}
