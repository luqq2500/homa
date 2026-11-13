package ie.port;

import java.time.Instant;

/** An integration event published by the payment context. */
public interface PaymentIE {

    Instant occurredAt();
}
