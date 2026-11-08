package ie.port;

import java.time.Instant;

/** An integration event published by account: registrations, verification results and re-verification. */
public interface AccountIE {

    /** When the event happened. */
    Instant occurredAt();
}
