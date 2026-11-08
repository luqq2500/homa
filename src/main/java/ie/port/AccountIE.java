package ie.port;

import java.time.Instant;

/** An integration event published by account: registrations and verification results. */
public interface AccountIE {

    /** When the event happened. */
    Instant occurredAt();
}
