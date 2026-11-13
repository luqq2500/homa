package ie.port;

import java.time.Instant;

/** An integration event published by the policy context. */
public interface PolicyIE {

    Instant occurredAt();
}
