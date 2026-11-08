package ie.events;

import ie.port.ReservationIE;
import java.time.Instant;
import java.util.Objects;
import kernel.Money;

/** A provider was fined for a late cancellation or a no-show. */
public record FineChargedIE(
        String providerId,
        String reservationId,
        Money amount,
        String reason,
        Instant occurredAt) implements ReservationIE {

    public FineChargedIE {
        Objects.requireNonNull(providerId, "providerId");
        Objects.requireNonNull(reservationId, "reservationId");
        Objects.requireNonNull(amount, "amount");
        Objects.requireNonNull(reason, "reason");
        Objects.requireNonNull(occurredAt, "occurredAt");
    }
}
