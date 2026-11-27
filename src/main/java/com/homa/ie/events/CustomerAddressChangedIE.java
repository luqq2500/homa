package com.homa.ie.events;

import com.homa.ie.port.CustomerIE;
import java.time.Instant;

/**
 * A customer changed their home address or their arrival note (CUS-1). Reservation copies the city for RES-1, and a
 * new reservation keeps the whole address with its note from when it is Requested (RES-8); booked ones keep theirs.
 *
 * @param city          the Homa city code of the home address (RES-1)
 * @param streetAddress the home address without its city, as the customer entered it (CUS-1)
 * @param arrivalNote   how the team reaches the home (CUS-1)
 */
public record CustomerAddressChangedIE(
        String eventId,
        String customerId, String city, String streetAddress, String arrivalNote, String status, Instant occurredAt)
        implements CustomerIE {
}
