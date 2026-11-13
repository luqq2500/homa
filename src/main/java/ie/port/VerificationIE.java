package ie.port;

import java.time.Instant;

/** An integration event published by the verification context. */
public interface VerificationIE {

    Instant occurredAt();
}
