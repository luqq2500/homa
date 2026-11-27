package com.homa.ie.events;

import com.homa.ie.port.ReservationIE;
import java.time.Instant;
import java.util.List;

/**
 * The provider set or changed a reservation's roster before the Check-in Period; the customer is told of every
 * change (ROS-1). Support keeps it for later reports; Provider keeps each member's rostered windows (HLP-9).
 *
 * @param memberIds the rostered helpers and supervisors, as Provider's team-member ids
 */
public record RosterChangedIE(String eventId, String reservationId, List<String> memberIds, Instant occurredAt)
        implements ReservationIE {

    public RosterChangedIE {
        memberIds = List.copyOf(memberIds);
    }
}
