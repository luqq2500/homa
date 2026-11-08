package ie.events;

import ie.port.ReservationIE;
import java.time.Instant;
import java.util.Objects;
import kernel.Money;

/** The customer challenged a customer no-show; Homa reviews it. */
public record NoShowChallengedIE(
        String reservationId,
        Money atStake,
        Instant occurredAt) implements ReservationIE {

    public NoShowChallengedIE {
        Objects.requireNonNull(reservationId, "reservationId");
        Objects.requireNonNull(atStake, "atStake");
        Objects.requireNonNull(occurredAt, "occurredAt");
    }
}
