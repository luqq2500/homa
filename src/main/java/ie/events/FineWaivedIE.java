package ie.events;

import ie.port.PaymentIE;
import java.time.Instant;
import java.util.Objects;

/** Homa waived what was left of a fine after review. */
public record FineWaivedIE(
        String fineId,
        String providerId,
        String reason,
        Instant occurredAt) implements PaymentIE {

    public FineWaivedIE {
        Objects.requireNonNull(fineId, "fineId");
        Objects.requireNonNull(providerId, "providerId");
        Objects.requireNonNull(reason, "reason");
        Objects.requireNonNull(occurredAt, "occurredAt");
    }
}
