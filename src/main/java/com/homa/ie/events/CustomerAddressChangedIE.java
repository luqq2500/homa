package com.homa.ie.events;

import com.homa.ie.port.CustomerIE;
import java.time.Instant;

/** A customer changed their home address; Reservation copies the city for RES-1. */
public record CustomerAddressChangedIE(String customerId, String city, String status, Instant occurredAt)
        implements CustomerIE {
}
