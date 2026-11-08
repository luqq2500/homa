package ie.events;

import ie.port.AccountIE;
import java.time.Instant;
import java.util.Objects;

/** Stripe reports a provider's payout account as ready. */
public record PayoutAccountReadyIE(
        String providerId,
        Instant occurredAt) implements AccountIE {

    public PayoutAccountReadyIE {
        Objects.requireNonNull(providerId, "providerId");
        Objects.requireNonNull(occurredAt, "occurredAt");
    }
}
