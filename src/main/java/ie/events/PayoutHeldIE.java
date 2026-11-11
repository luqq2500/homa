package ie.events;

import ie.port.PaymentIE;
import java.time.Instant;
import java.util.Objects;

/** A payout to a seriously flagged business is held for Homa's founders to review; it is not paid out. */
public record PayoutHeldIE(
        String payoutId,
        String providerId,
        String reason,
        Instant occurredAt) implements PaymentIE {

    public PayoutHeldIE {
        Objects.requireNonNull(payoutId, "payoutId");
        Objects.requireNonNull(providerId, "providerId");
        Objects.requireNonNull(reason, "reason");
        Objects.requireNonNull(occurredAt, "occurredAt");
    }
}
