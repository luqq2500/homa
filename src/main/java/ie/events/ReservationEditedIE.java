package ie.events;

import ie.port.ReservationIE;
import java.time.Instant;
import java.util.Objects;
import kernel.Money;

/** A customer edited a request before acceptance. */
public record ReservationEditedIE(
        String reservationId,
        Money previousTotal,
        Money newTotal,
        Money difference,
        Instant occurredAt) implements ReservationIE {

    public ReservationEditedIE {
        Objects.requireNonNull(reservationId, "reservationId");
        Objects.requireNonNull(previousTotal, "previousTotal");
        Objects.requireNonNull(newTotal, "newTotal");
        Objects.requireNonNull(difference, "difference");
        Objects.requireNonNull(occurredAt, "occurredAt");
    }
}
