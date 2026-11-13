package ie.events;

import ie.port.PaymentIE;
import java.time.Instant;
import java.util.Objects;

/** Homa's admins kept a held payout back after their review, with a reason. */
public record PayoutWithheldIE(
        String payoutId,
        String providerId,
        String reason,
        Instant occurredAt) implements PaymentIE {

    public PayoutWithheldIE {
        Objects.requireNonNull(payoutId, "payoutId");
        Objects.requireNonNull(providerId, "providerId");
        Objects.requireNonNull(reason, "reason");
        Objects.requireNonNull(occurredAt, "occurredAt");
    }
}
