package ie.events;

import ie.port.PaymentIE;
import java.time.Instant;
import java.util.Objects;

/** Homa's founders released a held payout after their review; it is paid out as normal. */
public record PayoutHoldReleasedIE(
        String payoutId,
        String providerId,
        Instant occurredAt) implements PaymentIE {

    public PayoutHoldReleasedIE {
        Objects.requireNonNull(payoutId, "payoutId");
        Objects.requireNonNull(providerId, "providerId");
        Objects.requireNonNull(occurredAt, "occurredAt");
    }
}
