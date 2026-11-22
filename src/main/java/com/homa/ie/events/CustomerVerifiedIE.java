package com.homa.ie.events;

import com.homa.ie.port.CustomerIE;
import java.time.Instant;

/**
 * A customer's identity is verified as local and aged 18 or above, so they can reserve (CUS-3, CUS-10).
 * Reservation updates its customer standing from it; it never consumes Verification's person events itself.
 */
public record CustomerVerifiedIE(String eventId, String customerId, String personId, Instant occurredAt)
        implements CustomerIE {
}
