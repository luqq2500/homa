package ie.port;

import java.time.Instant;

/** An integration event published by payment: captures, refunds, payouts and waived fines. */
public interface PaymentIE {

    /** When the event happened. */
    Instant occurredAt();
}
