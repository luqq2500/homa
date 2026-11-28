package com.homa.ie.events;

import com.homa.ie.port.ReservationIE;
import java.time.Instant;

/**
 * The customer declined the smaller roster the provider offered (ROS-8). Notification tells the provider, who can
 * still set a complete roster before {@code checkInStart}; if it doesn't, the reservation is cancelled under the No
 * Roster Policy when the Check-in Period starts (ROS-7). Nothing is refunded or fined by the decline itself.
 *
 * @param declinedAt   when the customer declined
 * @param checkInStart when the roster locks: the provider completes it before this moment
 */
public record SmallerRosterDeclinedIE(
        String eventId,
        String reservationId,
        String customerId,
        String providerId,
        Instant declinedAt,
        Instant checkInStart,
        Instant occurredAt) implements ReservationIE {
}
