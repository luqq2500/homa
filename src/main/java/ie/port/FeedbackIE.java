package ie.port;

import java.time.Instant;

/** An integration event published by feedback: customers' ratings of providers and helpers. */
public interface FeedbackIE {

    /** When the event happened. */
    Instant occurredAt();
}
