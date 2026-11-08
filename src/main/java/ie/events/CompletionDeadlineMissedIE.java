package ie.events;

import ie.port.ReservationIE;
import java.time.Instant;
import java.util.Objects;
import kernel.Money;

/** The provider missed the completion deadline; Homa reviews it. */
public record CompletionDeadlineMissedIE(
        String reservationId,
        Money atStake,
        Instant occurredAt) implements ReservationIE {

    public CompletionDeadlineMissedIE {
        Objects.requireNonNull(reservationId, "reservationId");
        Objects.requireNonNull(atStake, "atStake");
        Objects.requireNonNull(occurredAt, "occurredAt");
    }
}
