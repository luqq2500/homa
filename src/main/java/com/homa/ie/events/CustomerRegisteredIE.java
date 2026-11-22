package com.homa.ie.events;

import com.homa.ie.port.CustomerIE;
import java.time.Instant;

/**
 * A customer registered (CUS-1, CUS-2). Reservation and Conversation keep their customer standing from it. A new
 * customer isn't identity verified yet, so can't reserve until {@code CustomerVerifiedIE} (CUS-3).
 */
public record CustomerRegisteredIE(
        String eventId,
        String customerId, String city, String status, Instant occurredAt)
        implements CustomerIE {
}
