package com.homa.ie.events;

import com.homa.ie.port.CustomerIE;
import java.time.Instant;

/**
 * A customer registered (CUS-1, CUS-2). Reservation and Conversation keep their customer standing from it, and
 * Reservation the full home address a reservation keeps from when it is Requested (RES-8). A new customer isn't
 * identity verified yet, so can't reserve until {@code CustomerVerifiedIE} (CUS-3).
 *
 * @param city          the Homa city code of the home address (RES-1)
 * @param streetAddress the home address without its city, as the customer entered it (CUS-1)
 * @param arrivalNote   how the team reaches the home, such as "Guardhouse: say unit B-12-3" (CUS-1)
 */
public record CustomerRegisteredIE(
        String eventId,
        String customerId, String city, String streetAddress, String arrivalNote, String status, Instant occurredAt)
        implements CustomerIE {
}
