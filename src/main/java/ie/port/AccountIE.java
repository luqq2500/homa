package ie.port;

import java.time.Instant;

/** An integration event published by the account context. */
public interface AccountIE {

    Instant occurredAt();
}
